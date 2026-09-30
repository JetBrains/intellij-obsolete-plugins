package com.intellij.guice;

import static com.google.common.truth.Truth.assertThat;

import com.intellij.guice.model.GuiceInjectorManager;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.beans.BindToDescriptor;
import com.intellij.guice.model.beans.BindToProviderDescriptor;
import com.intellij.guice.model.beans.MapMultibindDescriptor;
import com.intellij.guice.model.beans.OptionalBindDescriptor;
import com.intellij.guice.model.beans.UntargetedBindDescriptor;
import com.intellij.guice.model.extensions.GuiceBindingContributor;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UCallExpression;

public class GuiceBinderTest extends GuiceTestBase {

  public void testJavaSimpleBinding() {
    PsiFile file = myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(MyService.class).to(MyServiceImpl.class);
        }
      }
      """);

    Set<BindDescriptor> bindings = GuiceInjectorManager.getBindingsInFile(file);
    assertEquals(1, bindings.size());
    BindDescriptor binding = bindings.iterator().next();
    assertThat(binding).isInstanceOf(BindToDescriptor.class);
    assertEquals("MyService", binding.getBoundClass().getQualifiedName());
    assertEquals("MyServiceImpl", binding.getBindingClass().getQualifiedName());
  }

  public void testKotlinSimpleBinding() {
    PsiFile file = myFixture.configureByText("MyModule.kt", """
      import com.google.inject.AbstractModule
      class MyModule : AbstractModule() {
        override fun configure() {
          bind(MyService::class.java).to(MyServiceImpl::class.java)
        }
      }
      """);

    Set<BindDescriptor> bindings = GuiceInjectorManager.getBindingsInFile(file);
    assertEquals(1, bindings.size());
    BindDescriptor binding = bindings.iterator().next();
    assertThat(binding).isInstanceOf(BindToDescriptor.class);
    assertEquals("MyService", binding.getBoundClass().getQualifiedName());
    assertEquals("MyServiceImpl", binding.getBindingClass().getQualifiedName());
  }

  public void testJavaUntargetedBinding() {
    PsiFile file = myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(MyServiceImpl.class);
        }
      }
      """);

    Set<BindDescriptor> bindings = GuiceInjectorManager.getBindingsInFile(file);
    assertEquals(1, bindings.size());
    BindDescriptor binding = bindings.iterator().next();
    assertThat(binding).isInstanceOf(UntargetedBindDescriptor.class);
    assertEquals("MyServiceImpl", binding.getBoundClass().getQualifiedName());
  }



  public void testJavaProviderBinding() {
    PsiFile file = myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(MyService.class).toProvider(MyServiceProvider.class);
        }
      }
      """);

    Set<BindDescriptor> bindings = GuiceInjectorManager.getBindingsInFile(file);
    assertEquals(1, bindings.size());
    BindDescriptor binding = bindings.iterator().next();
    assertThat(binding).isInstanceOf(BindToProviderDescriptor.class);
    assertEquals("MyService", binding.getBoundClass().getQualifiedName());
    assertEquals("MyService", binding.getBindingClass().getQualifiedName());
    assertEquals("MyServiceProvider", ((BindToProviderDescriptor) binding).getProviderClass().getQualifiedName());
  }

  public void testJavaConstructorBinding() {
    PsiFile file = myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          try {
            bind(MyService.class).toConstructor(MyServiceImpl.class.getConstructor());
          } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
          }
        }
      }
      """);

    Set<BindDescriptor> bindings = GuiceInjectorManager.getBindingsInFile(file);
    assertEquals(1, bindings.size());
    BindDescriptor binding = bindings.iterator().next();
    assertThat(binding).isInstanceOf(com.intellij.guice.model.beans.BindToConstructorDescriptor.class);
    assertEquals("MyService", binding.getBoundClass().getQualifiedName());
    assertEquals("MyServiceImpl", binding.getBindingClass().getQualifiedName());
  }

  public void testJavaUnresolvedTargetClass() {
    PsiFile file = myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(MyService.class).to(ClassThatDoNotExist.class);
        }
      }
      """);

    Set<BindDescriptor> bindings = GuiceInjectorManager.getBindingsInFile(file);
    assertEquals(1, bindings.size());
    BindDescriptor binding = bindings.iterator().next();
    assertThat(binding).isInstanceOf(BindToDescriptor.class);
    assertEquals("MyService", binding.getBoundClass().getQualifiedName());
    assertNull(binding.getBindingClass());
  }

  public void testJavaUnresolvedProviderClass() {
    PsiFile file = myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(MyService.class).toProvider(DoNotExistProvider.class);
        }
      }
      """);

    Set<BindDescriptor> bindings = GuiceInjectorManager.getBindingsInFile(file);
    assertEquals(1, bindings.size());
    BindDescriptor binding = bindings.iterator().next();
    assertThat(binding).isInstanceOf(BindToProviderDescriptor.class);
    assertEquals("MyService", binding.getBoundClass().getQualifiedName());
    assertNull(((BindToProviderDescriptor) binding).getProviderClass());
    assertNull(binding.getBindingClass());
  }

  public void testOptionalBinderWithUnresolvedClass() {
    PsiFile file = myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.multibindings.OptionalBinder;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          OptionalBinder.newOptionalBinder(binder(), DoNotExist.class);
        }
      }
      """);

    Set<BindDescriptor> bindings = GuiceInjectorManager.getBindingsInFile(file);
    assertEquals(1, bindings.size());
    BindDescriptor binding = bindings.iterator().next();
    assertThat(binding).isInstanceOf(OptionalBindDescriptor.class);
    assertNull(((OptionalBindDescriptor) binding).getOptionalBoundClass());
  }

  public void testMapBinderWithUnresolvedKeyAndValueClass() {
    PsiFile file = myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.multibindings.MapBinder;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          MapBinder.newMapBinder(binder(), DoNotExistKey.class, DoNotExistValue.class);
        }
      }
      """);

    Set<BindDescriptor> bindings = GuiceInjectorManager.getBindingsInFile(file);
    assertEquals(1, bindings.size());
    BindDescriptor binding = bindings.iterator().next();
    assertThat(binding).isInstanceOf(MapMultibindDescriptor.class);
    assertNull(((MapMultibindDescriptor) binding).getKeyType());
    assertNull(((MapMultibindDescriptor) binding).getValueType());
  }
  public void testThrowingProviderBinderIsNotAStandardBinding() {
    myFixture.addClass("""
      package com.google.inject.throwingproviders;
      public class ThrowingProviderBinder {
        public static ThrowingProviderBinder create(com.google.inject.Binder binder) { return null; }
        public <P, T> SecondaryBinder<P, T> bind(Class<P> interfaceType, Class<T> valueType) { return null; }
        public static class SecondaryBinder<P, T> {
          public void to(Class<? extends P> targetType) {}
        }
      }
      """);
    PsiFile file = myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.throwingproviders.ThrowingProviderBinder;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          ThrowingProviderBinder.create(binder()).bind(MyKey.class, MyService.class).to(MyServiceProvider.class);
        }
      }
      """);

    // The key of this binding is MyKey<MyService>, not MyService. The plugin does not model it.
    assertThat(GuiceInjectorManager.getBindingsInFile(file)).isEmpty();
  }
  public void testJavaAnonymousModule() {
    PsiFile file = myFixture.configureByText("Main.java", """
      import com.google.inject.AbstractModule;
      public class Main {
        Object module() {
          return new AbstractModule() {
            @Override
            protected void configure() {
              bind(MyService.class).to(MyServiceImpl.class);
            }
          };
        }
      }
      """);

    Set<BindDescriptor> bindings = GuiceInjectorManager.getBindingsInFile(file);
    assertEquals(1, bindings.size());
    BindDescriptor binding = bindings.iterator().next();
    assertEquals("MyService", binding.getBoundClass().getQualifiedName());
    assertEquals("MyServiceImpl", binding.getBindingClass().getQualifiedName());
  }

  public void testKotlinObjectModule() {
    PsiFile file = myFixture.configureByText("Main.kt", """
      import com.google.inject.AbstractModule
      fun module() = object : AbstractModule() {
        override fun configure() {
          bind(MyService::class.java).to(MyServiceImpl::class.java)
        }
      }
      """);

    Set<BindDescriptor> bindings = GuiceInjectorManager.getBindingsInFile(file);
    assertEquals(1, bindings.size());
    BindDescriptor binding = bindings.iterator().next();
    assertEquals("MyService", binding.getBoundClass().getQualifiedName());
    assertEquals("MyServiceImpl", binding.getBindingClass().getQualifiedName());
  }

  public void testUnresolvedNonBindingChainDoesNotProduceDescriptor() {
    PsiFile file = myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          unresolvedHelper().to(MyServiceImpl.class);
        }
      }
      """);

    assertThat(GuiceInjectorManager.getBindingsInFile(file)).isEmpty();
  }

  public void testDescriptorDoesNotRetainLazyPsiAndResolvesAddedTargetClass() {
    assertThat(Arrays.stream(BindDescriptor.class.getDeclaredFields()).map(Field::getType))
      .doesNotContain(NullableLazyValue.class);
    assertThat(Arrays.stream(BindToProviderDescriptor.class.getDeclaredFields()).map(Field::getType))
      .doesNotContain(NullableLazyValue.class);

    PsiFile file = myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(MyService.class).to(LateImpl.class);
        }
      }
      """);

    Set<BindDescriptor> initialBindings = GuiceInjectorManager.getBindingsInFile(file);
    assertEquals(1, initialBindings.size());
    BindDescriptor binding = initialBindings.iterator().next();
    assertNull(binding.getBindingClass());

    myFixture.addClass("public class LateImpl implements MyService {}");

    assertNotNull(binding.getBindingClass());
    assertEquals("LateImpl", binding.getBindingClass().getQualifiedName());
  }

  public void testDynamicContributorRegistrationInvalidatesBindingsCache() {
    PsiFile file = myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        protected void customBind(Class<?> clazz) {}
        @Override
        protected void configure() {
          customBind(MyServiceImpl.class);
        }
      }
      """);

    assertThat(GuiceInjectorManager.getBindingsInFile(file)).isEmpty();

    GuiceBindingContributor customContributor = new GuiceBindingContributor() {
      @Override
      public @NotNull Set<String> getBindingWords() {
        return Set.of("customBind");
      }

      @Override
      public boolean processCall(@NotNull UCallExpression call,
                                 @NotNull String methodName,
                                 @NotNull String resolvedQName,
                                 @NotNull PsiClass containingClass,
                                 @NotNull Set<BindDescriptor> descriptors) {
        if (call.getSourcePsi() != null) {
          descriptors.add(new BindDescriptor(call.getSourcePsi()) {
            @Override
            public PsiClass calculateBindingClass() {
              return containingClass;
            }
          });
          return true;
        }
        return false;
      }
    };

    GuiceBindingContributor.EP_NAME.getPoint().registerExtension(customContributor, getTestRootDisposable());

    Set<BindDescriptor> updatedBindings = GuiceInjectorManager.getBindingsInFile(file);
    assertEquals(1, updatedBindings.size());
    BindDescriptor binding = updatedBindings.iterator().next();
    assertEquals("MyModule", binding.getBindingClass().getQualifiedName());
  }

  public void testAssistedFactoryThreeArgImplement() {
    PsiFile file = myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.assistedinject.FactoryModuleBuilder;
      import com.google.inject.name.Names;
      public class MyModule extends AbstractModule {
        public interface MyFactory { MyService create(); }
        @Override
        protected void configure() {
          install(new FactoryModuleBuilder()
              .implement(MyService.class, Names.named("fast"), MyServiceImpl.class)
              .build(MyFactory.class));
        }
      }
      """);

    Set<BindDescriptor> bindings = GuiceInjectorManager.getBindingsInFile(file);
    assertEquals(1, bindings.size());
    BindDescriptor binding = bindings.iterator().next();
    assertNotNull(binding.getBindingClass());
    assertEquals("MyServiceImpl", binding.getBindingClass().getQualifiedName());
  }
}

