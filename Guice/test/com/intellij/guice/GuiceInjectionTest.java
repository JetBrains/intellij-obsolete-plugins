package com.intellij.guice;

import static com.google.common.truth.Truth.assertThat;

import com.intellij.guice.model.GuiceLiveIndex;
import com.intellij.guice.model.InjectionPointDescriptor;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.beans.BindToDescriptor;
import com.intellij.guice.model.beans.BindToProviderDescriptor;
import com.intellij.guice.model.beans.MapMultibindDescriptor;
import com.intellij.guice.model.beans.OptionalBindDescriptor;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.search.GlobalSearchScope;
import java.util.Set;

public class GuiceInjectionTest extends GuiceTestBase {

  public void testJavaInjectFieldToSimpleBinding() {
    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(MyService.class).to(MyServiceImpl.class);
        }
      }
      """);

    PsiClass clientClass = myFixture.addClass("""
      import com.google.inject.Inject;
      public class Client {
        @Inject private MyService service;
      }
      """);

    PsiField field = clientClass.findFieldByName("service", false);
    assertNotNull(field);
    InjectionPointDescriptor ip = new InjectionPointDescriptor(field);

    GuiceLiveIndex liveIndex = getIndex();

    Set<BindDescriptor> bindings = liveIndex.findMatchingBindings(ip);
    assertEquals(1, bindings.size());
    BindDescriptor binding = bindings.iterator().next();
    assertThat(binding).isInstanceOf(BindToDescriptor.class);
    assertEquals("MyService", binding.getBoundClass().getQualifiedName());
    assertEquals("MyServiceImpl", binding.getBindingClass().getQualifiedName());
  }

  public void testJavaInjectFieldToProviderBinding() {
    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(MyService.class).toProvider(MyServiceProvider.class);
        }
      }
      """);

    PsiClass clientClass = myFixture.addClass("""
      import com.google.inject.Inject;
      public class Client {
        @Inject private MyService service;
      }
      """);

    GuiceLiveIndex liveIndex = getIndex();

    PsiField field = clientClass.findFieldByName("service", false);
    assertNotNull(field);
    InjectionPointDescriptor ip = new InjectionPointDescriptor(field);

    Set<BindDescriptor> bindings = liveIndex.findMatchingBindings(ip);
    assertEquals(1, bindings.size());
    BindDescriptor binding = bindings.iterator().next();
    assertThat(binding).isInstanceOf(BindToProviderDescriptor.class);
    assertEquals("MyService", binding.getBoundClass().getQualifiedName());
    assertEquals("MyServiceProvider", ((BindToProviderDescriptor) binding).getProviderClass().getQualifiedName());
  }

  public void testJavaInjectConstructorParameter() {
    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(MyService.class).to(MyServiceImpl.class);
        }
      }
      """);

    PsiClass clientClass = myFixture.addClass("""
      import com.google.inject.Inject;
      public class Client {
        private final MyService service;
        @Inject
        public Client(MyService service) {
          this.service = service;
        }
      }
      """);

    GuiceLiveIndex liveIndex = getIndex();

    var constructors = clientClass.getConstructors();
    assertEquals(1, constructors.length);
    var parameters = constructors[0].getParameterList().getParameters();
    assertEquals(1, parameters.length);
    PsiParameter parameter = parameters[0];

    InjectionPointDescriptor ip = new InjectionPointDescriptor(parameter);

    Set<BindDescriptor> bindings = liveIndex.findMatchingBindings(ip);
    assertEquals(1, bindings.size());
    BindDescriptor binding = bindings.iterator().next();
    assertThat(binding).isInstanceOf(BindToDescriptor.class);
    assertEquals("MyService", binding.getBoundClass().getQualifiedName());
  }

  public void testJavaInjectFieldToOptionalBinder() {
    myFixture.addClass("""
      package java.util;
      public final class Optional<T> {
        public static <T> Optional<T> empty() { return null; }
      }
      """);

    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.multibindings.OptionalBinder;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          OptionalBinder.newOptionalBinder(binder(), MyService.class).setDefault().to(MyServiceImpl.class);
        }
      }
      """);

    PsiClass clientClass = myFixture.addClass("""
      import com.google.inject.Inject;
      import java.util.Optional;
      public class Client {
        @Inject private Optional<MyService> service;
      }
      """);

    GuiceLiveIndex liveIndex = getIndex();

    PsiField field = clientClass.findFieldByName("service", false);
    assertNotNull(field);
    InjectionPointDescriptor ip = new InjectionPointDescriptor(field);

    Set<BindDescriptor> bindings = liveIndex.findMatchingBindings(ip);
    assertEquals(1, bindings.size());
    BindDescriptor binding = bindings.iterator().next();
    assertThat(binding).isInstanceOf(OptionalBindDescriptor.class);
    assertEquals("MyService", binding.getBoundClass().getQualifiedName());
  }

  public void testJavaInjectFieldToMapBinder() {
    myFixture.addClass("""
      package java.util;
      public interface Map<K, V> {}
      """);

    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.multibindings.MapBinder;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          MapBinder.newMapBinder(binder(), MyKey.class, MyService.class).addBinding(null).to(MyServiceImpl.class);
        }
      }
      """);

    PsiClass clientClass = myFixture.addClass("""
      import com.google.inject.Inject;
      import java.util.Map;
      public class Client {
        @Inject private Map<MyKey, MyService> serviceMap;
      }
      """);

    GuiceLiveIndex liveIndex = getIndex();

    PsiField field = clientClass.findFieldByName("serviceMap", false);
    assertNotNull(field);
    InjectionPointDescriptor ip = new InjectionPointDescriptor(field);

    Set<BindDescriptor> bindings = liveIndex.findMatchingBindings(ip);
    assertEquals(1, bindings.size());
    BindDescriptor binding = bindings.iterator().next();
    assertThat(binding).isInstanceOf(MapMultibindDescriptor.class);
    assertEquals("MyKey", ((MapMultibindDescriptor) binding).getKeyType().getQualifiedName());
    assertEquals("MyService", ((MapMultibindDescriptor) binding).getValueType().getQualifiedName());
  }

  public void testKotlinInjectFieldToSimpleBinding() {
    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(MyService.class).to(MyServiceImpl.class);
        }
      }
      """);

    myFixture.configureByText("Client.kt", """
      import com.google.inject.Inject
      class Client {
        @Inject
        private lateinit var service: MyService
      }
      """);

    PsiClass clientClass = JavaPsiFacade.getInstance(getProject())
        .findClass("Client", GlobalSearchScope.projectScope(getProject()));
    assertNotNull(clientClass);

    GuiceLiveIndex liveIndex = getIndex();

    PsiField field = clientClass.findFieldByName("service", false);
    assertNotNull(field);
    InjectionPointDescriptor ip = new InjectionPointDescriptor(field);

    Set<BindDescriptor> bindings = liveIndex.findMatchingBindings(ip);
    assertEquals(1, bindings.size());
    BindDescriptor binding = bindings.iterator().next();
    assertThat(binding).isInstanceOf(BindToDescriptor.class);
    assertEquals("MyService", binding.getBoundClass().getQualifiedName());
  }

  public void testJavaInjectFieldToUnresolvedAnnotationBinding() {
    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(MyService.class).annotatedWith(DoNotExistAnno.class).to(MyServiceImpl.class);
        }
      }
      """);

    PsiClass clientClass = myFixture.addClass("""
      import com.google.inject.Inject;
      public class Client {
        @Inject private MyService service; // No qualifier
      }
      """);

    GuiceLiveIndex liveIndex = getIndex();

    PsiField field = clientClass.findFieldByName("service", false);
    assertNotNull(field);
    InjectionPointDescriptor ip = new InjectionPointDescriptor(field);

    Set<BindDescriptor> bindings = liveIndex.findMatchingBindings(ip);
    // Should NOT match because the binding explicitly has a qualifier (even though it's unresolvable)
    assertEmpty(bindings);
  }
}
