package com.intellij.guice;

import static com.google.common.truth.Truth.assertThat;

import com.intellij.guice.model.EntryRole;
import com.intellij.guice.model.GuiceEntry;
import com.intellij.guice.model.GuiceEntryProducer;
import com.intellij.guice.model.GuiceNavigationIndex;
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

    GuiceNavigationIndex index = getNavigationIndex();
    Set<GuiceEntry> clientEntries = GuiceEntryProducer.extractFromClass(clientClass);
    GuiceEntry fieldEntry = clientEntries.stream()
        .filter(e -> e.getRole() == EntryRole.INJECTION_POINT && e.getNavigationTarget() == field)
        .findFirst()
        .orElse(null);
    assertNotNull(fieldEntry);

    Set<GuiceEntry> counterparts = index.findCounterparts(fieldEntry);
    assertEquals(1, counterparts.size());
    GuiceEntry counterpart = counterparts.iterator().next();
    assertEquals(EntryRole.BINDING_SITE, counterpart.getRole());
    assertEquals("bind(MyService.class).to(MyServiceImpl.class)", counterpart.getPresentableText());
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

    PsiField field = clientClass.findFieldByName("service", false);
    assertNotNull(field);

    GuiceNavigationIndex index = getNavigationIndex();
    Set<GuiceEntry> clientEntries = GuiceEntryProducer.extractFromClass(clientClass);
    GuiceEntry fieldEntry = clientEntries.stream()
        .filter(e -> e.getRole() == EntryRole.INJECTION_POINT && e.getNavigationTarget() == field)
        .findFirst()
        .orElse(null);
    assertNotNull(fieldEntry);

    Set<GuiceEntry> counterparts = index.findCounterparts(fieldEntry);
    assertEquals(1, counterparts.size());
    GuiceEntry counterpart = counterparts.iterator().next();
    assertEquals(EntryRole.BINDING_SITE, counterpart.getRole());
    assertEquals("bind(MyService.class).toProvider(MyServiceProvider.class)", counterpart.getPresentableText());
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

    var constructors = clientClass.getConstructors();
    assertEquals(1, constructors.length);
    var parameters = constructors[0].getParameterList().getParameters();
    assertEquals(1, parameters.length);
    PsiParameter parameter = parameters[0];

    GuiceNavigationIndex index = getNavigationIndex();
    Set<GuiceEntry> clientEntries = GuiceEntryProducer.extractFromClass(clientClass);
    GuiceEntry paramEntry = clientEntries.stream()
        .filter(e -> e.getRole() == EntryRole.INJECTION_POINT && e.getNavigationTarget() == parameter)
        .findFirst()
        .orElse(null);
    assertNotNull(paramEntry);

    Set<GuiceEntry> counterparts = index.findCounterparts(paramEntry);
    assertEquals(1, counterparts.size());
    GuiceEntry counterpart = counterparts.iterator().next();
    assertEquals(EntryRole.BINDING_SITE, counterpart.getRole());
    assertEquals("bind(MyService.class).to(MyServiceImpl.class)", counterpart.getPresentableText());
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

    PsiField field = clientClass.findFieldByName("service", false);
    assertNotNull(field);

    GuiceNavigationIndex index = getNavigationIndex();
    Set<GuiceEntry> clientEntries = GuiceEntryProducer.extractFromClass(clientClass);
    GuiceEntry fieldEntry = clientEntries.stream()
        .filter(e -> e.getRole() == EntryRole.INJECTION_POINT && e.getNavigationTarget() == field)
        .findFirst()
        .orElse(null);
    assertNotNull(fieldEntry);

    Set<GuiceEntry> counterparts = index.findCounterparts(fieldEntry);
    assertEquals(1, counterparts.size());
    GuiceEntry counterpart = counterparts.iterator().next();
    assertEquals(EntryRole.BINDING_SITE, counterpart.getRole());
    assertEquals("Optional<MyService>", counterpart.getPresentableText());
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

    PsiField field = clientClass.findFieldByName("serviceMap", false);
    assertNotNull(field);

    GuiceNavigationIndex index = getNavigationIndex();
    Set<GuiceEntry> clientEntries = GuiceEntryProducer.extractFromClass(clientClass);
    GuiceEntry fieldEntry = clientEntries.stream()
        .filter(e -> e.getRole() == EntryRole.INJECTION_POINT && e.getNavigationTarget() == field)
        .findFirst()
        .orElse(null);
    assertNotNull(fieldEntry);

    Set<GuiceEntry> counterparts = index.findCounterparts(fieldEntry);
    assertEquals(1, counterparts.size());
    GuiceEntry counterpart = counterparts.iterator().next();
    assertEquals(EntryRole.BINDING_SITE, counterpart.getRole());
    assertEquals("Map<MyKey, MyService>", counterpart.getPresentableText());
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

    PsiField field = clientClass.findFieldByName("service", false);
    assertNotNull(field);

    GuiceNavigationIndex index = getNavigationIndex();
    Set<GuiceEntry> clientEntries = GuiceEntryProducer.extractFromClass(clientClass);


    GuiceEntry fieldEntry = clientEntries.stream()
        .filter(e -> e.getRole() == EntryRole.INJECTION_POINT && e.getNavigationTarget().equals(field))
        .findFirst()
        .orElse(null);
    assertNotNull(fieldEntry);

    Set<GuiceEntry> counterparts = index.findCounterparts(fieldEntry);
    assertEquals(1, counterparts.size());
    GuiceEntry counterpart = counterparts.iterator().next();
    assertEquals(EntryRole.BINDING_SITE, counterpart.getRole());
    assertEquals("bind(MyService.class).to(MyServiceImpl.class)", counterpart.getPresentableText());
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

    PsiField field = clientClass.findFieldByName("service", false);
    assertNotNull(field);

    GuiceNavigationIndex index = getNavigationIndex();
    Set<GuiceEntry> clientEntries = GuiceEntryProducer.extractFromClass(clientClass);
    GuiceEntry fieldEntry = clientEntries.stream()
        .filter(e -> e.getRole() == EntryRole.INJECTION_POINT && e.getNavigationTarget() == field)
        .findFirst()
        .orElse(null);
    assertNotNull(fieldEntry);

    Set<GuiceEntry> counterparts = index.findCounterparts(fieldEntry);
    assertEmpty(counterparts);
  }
}
