package com.intellij.guice;

import static com.google.common.truth.Truth.assertThat;

import com.intellij.guice.model.GuiceInjectorManager;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.beans.BindToDescriptor;
import com.intellij.guice.model.beans.BindToProviderDescriptor;
import com.intellij.guice.model.beans.MapMultibindDescriptor;
import com.intellij.guice.model.beans.OptionalBindDescriptor;
import com.intellij.guice.model.beans.UntargetedBindDescriptor;
import com.intellij.psi.PsiFile;
import java.util.Set;

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
}
