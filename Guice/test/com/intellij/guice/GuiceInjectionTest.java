package com.intellij.guice;

import com.intellij.codeInsight.daemon.GutterMark;
import com.intellij.codeInsight.daemon.LineMarkerInfo.LineMarkerGutterIconRenderer;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconRenderer;
import com.intellij.codeInsight.navigation.impl.PsiTargetPresentationRenderer;
import com.intellij.guice.model.EntryRole;
import com.intellij.guice.model.GuiceBindingClassAnnotator;
import com.intellij.guice.model.GuiceEntry;
import com.intellij.guice.model.GuiceEntryProducer;
import com.intellij.guice.model.GuiceInjectorManager;
import com.intellij.guice.model.GuiceProjectModel;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.extensions.GuiceBindingContributor;
import com.intellij.guice.model.extensions.GuiceCallPattern;
import com.intellij.guice.model.extensions.GuiceExtensionRegistrar;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.util.PsiTreeUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UCallExpression;

import static com.google.common.truth.Truth.assertThat;

/**
 * Checks injection-point gutters as the user sees them: which elements get an icon, and where the icon navigates.
 * The tests use no index internals, so a new storage model must pass them unchanged.
 */
public class GuiceInjectionTest extends GuiceTestBase {

  private void addModule(String configureBody) {
    myFixture.addFileToProject("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.multibindings.*;
      import com.google.inject.name.Names;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          %s
        }
      }
      """.formatted(configureBody));
  }

  public void testJavaInjectFieldToSimpleBinding() {
    addModule("bind(MyService.class).to(MyServiceImpl.class);");
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        @Inject private MyService service;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("service");
    assertThat(gutterTargets(TO_BINDINGS_TOOLTIP)).containsExactly("bind(MyService.class).to(MyServiceImpl.class)");
  }

  public void testSimpleBindingNavigatesBackToField() {
    myFixture.addFileToProject("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        @Inject private MyService service;
      }
      """);
    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(MyService.class).to(MyServiceImpl.class);
        }
      }
      """);

    assertThat(gutterTargets(TO_INJECTION_POINTS_TOOLTIP)).contains("@Inject private MyService service;");
  }

  public void testJavaInjectFieldToProviderBinding() {
    addModule("bind(MyService.class).toProvider(MyServiceProvider.class);");
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        @Inject private MyService service;
      }
      """);

    assertThat(gutterTargets(TO_BINDINGS_TOOLTIP))
      .containsExactly("bind(MyService.class).toProvider(MyServiceProvider.class)");
  }

  public void testJavaInjectConstructorParameter() {
    addModule("bind(MyService.class).to(MyServiceImpl.class);");
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        private final MyService service;
        @Inject
        public Client(MyService service) {
          this.service = service;
        }
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("service");
    assertThat(gutterTargets(TO_BINDINGS_TOOLTIP)).containsExactly("bind(MyService.class).to(MyServiceImpl.class)");
  }

  public void testJavaInjectProviderFieldToSimpleBinding() {
    addModule("bind(MyService.class).to(MyServiceImpl.class);");
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import javax.inject.Provider;
      public class Client {
        @Inject private Provider<MyService> service;
      }
      """);

    assertThat(gutterTargets(TO_BINDINGS_TOOLTIP)).containsExactly("bind(MyService.class).to(MyServiceImpl.class)");
  }

  public void testJavaInjectFieldToProvidesMethod() {
    myFixture.addFileToProject("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.Provides;
      public class MyModule extends AbstractModule {
        @Provides
        MyService provideService() { return null; }
      }
      """);
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        @Inject private MyService service;
      }
      """);

    assertThat(gutterTargets(TO_BINDINGS_TOOLTIP)).containsExactly("@Provides MyService provideService() { return null; }");
  }

  public void testJavaInjectFieldToOptionalBinder() {
    addModule("OptionalBinder.newOptionalBinder(binder(), MyService.class).setDefault().to(MyServiceImpl.class);");
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import java.util.Optional;
      public class Client {
        @Inject private Optional<MyService> service;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("service");
    assertThat(gutterTargets(TO_BINDINGS_TOOLTIP)).isNotEmpty();
  }

  public void testJavaInjectFieldToMapBinder() {
    addModule("MapBinder.newMapBinder(binder(), MyKey.class, MyService.class).addBinding(null).to(MyServiceImpl.class);");
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import java.util.Map;
      public class Client {
        @Inject private Map<MyKey, MyService> serviceMap;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("serviceMap");
    assertThat(gutterTargets(TO_BINDINGS_TOOLTIP)).isNotEmpty();
  }

  public void testJavaInjectFieldWithOtherTypeHasNoGutter() {
    addModule("bind(MyService.class).to(MyServiceImpl.class);");
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        @Inject private MyKey key;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).isEmpty();
  }

  public void testKotlinInjectFieldToSimpleBinding() {
    addModule("bind(MyService.class).to(MyServiceImpl.class);");
    myFixture.configureByText("Client.kt", """
      import com.google.inject.Inject
      class Client {
        @Inject
        private lateinit var service: MyService
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("service");
    assertThat(gutterTargets(TO_BINDINGS_TOOLTIP)).containsExactly("bind(MyService.class).to(MyServiceImpl.class)");
  }

  public void testKotlinInjectConstructorParameter() {
    addModule("bind(MyService.class).to(MyServiceImpl.class);");
    myFixture.configureByText("Client.kt", """
      import com.google.inject.Inject
      class Client @Inject constructor(private val service: MyService)
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("service");
    assertThat(gutterTargets(TO_BINDINGS_TOOLTIP)).containsExactly("bind(MyService.class).to(MyServiceImpl.class)");
  }

  public void testJavaInjectFieldToUnresolvedAnnotationBinding() {
    addModule("bind(MyService.class).annotatedWith(DoNotExistAnno.class).to(MyServiceImpl.class);");
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        @Inject private MyService service; // No qualifier
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).isEmpty();
  }

  // -----------------------------------------------------------------------
  // Qualifiers
  // -----------------------------------------------------------------------

  private static final String PLAIN_AND_NAMED_BINDINGS = """
    bind(MyService.class).to(MyServiceImpl.class);
    bind(MyService.class).annotatedWith(Names.named("db")).to(MyServiceImpl.class);
    """;

  public void testGuiceNamedFieldLinksOnlyToNamedBinding() {
    addModule(PLAIN_AND_NAMED_BINDINGS);
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import com.google.inject.name.Named;
      public class Client {
        @Inject @Named("db") private MyService service;
      }
      """);

    assertThat(gutterTargets(TO_BINDINGS_TOOLTIP))
      .containsExactly("bind(MyService.class).annotatedWith(Names.named(\"db\")).to(MyServiceImpl.class)");
  }

  public void testUnqualifiedFieldDoesNotLinkToNamedBinding() {
    addModule(PLAIN_AND_NAMED_BINDINGS);
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        @Inject private MyService service;
      }
      """);

    assertThat(gutterTargets(TO_BINDINGS_TOOLTIP)).containsExactly("bind(MyService.class).to(MyServiceImpl.class)");
  }

  public void testJavaxNamedFieldLinksToGuiceNamedBinding() {
    addModule(PLAIN_AND_NAMED_BINDINGS);
    myFixture.configureByText("Client.java", """
      import javax.inject.Inject;
      import javax.inject.Named;
      public class Client {
        @Inject @Named("db") private MyService service;
      }
      """);

    assertThat(gutterTargets(TO_BINDINGS_TOOLTIP))
      .containsExactly("bind(MyService.class).annotatedWith(Names.named(\"db\")).to(MyServiceImpl.class)");
  }

  public void testNamedFieldWithOtherNameHasNoGutter() {
    addModule(PLAIN_AND_NAMED_BINDINGS);
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import com.google.inject.name.Named;
      public class Client {
        @Inject @Named("cache") private MyService service;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).isEmpty();
  }

  public void testNamedBindingWithConstantAndStaticImport() {
    myFixture.addFileToProject("MyModule.java", """
      import com.google.inject.AbstractModule;
      import static com.google.inject.name.Names.named;
      public class MyModule extends AbstractModule {
        static final String DB = "d" + "b";
        @Override
        protected void configure() {
          bind(MyService.class).annotatedWith(named(DB)).to(MyServiceImpl.class);
        }
      }
      """);
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import com.google.inject.name.Named;
      public class Client {
        @Inject @Named("db") private MyService db;
        @Inject @Named("cache") private MyService cache;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("db");
  }

  public void testNamedBindingViaKey() {
    addModule("bind(com.google.inject.Key.get(MyService.class, Names.named(\"db\"))).to(MyServiceImpl.class);");
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import com.google.inject.name.Named;
      public class Client {
        @Inject @Named("db") private MyService db;
        @Inject private MyService plain;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("db");
  }

  public void testNamedProvidesMethod() {
    myFixture.addFileToProject("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.Provides;
      import com.google.inject.name.Named;
      public class MyModule extends AbstractModule {
        @Provides @Named("db")
        MyService provideDb() { return null; }
      }
      """);
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import com.google.inject.name.Named;
      public class Client {
        @Inject @Named("db") private MyService db;
        @Inject private MyService plain;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("db");
  }

  public void testMarkerBindingAnnotation() {
    myFixture.addClass("""
      import java.lang.annotation.*;
      @com.google.inject.BindingAnnotation
      @Retention(RetentionPolicy.RUNTIME)
      public @interface Db {}
      """);
    addModule(PLAIN_AND_NAMED_BINDINGS + "bind(MyService.class).annotatedWith(Db.class).to(MyServiceImpl.class);");
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        @Inject @Db private MyService service;
      }
      """);

    assertThat(gutterTargets(TO_BINDINGS_TOOLTIP))
      .containsExactly("bind(MyService.class).annotatedWith(Db.class).to(MyServiceImpl.class)");
  }

  public void testAssistedParameterIsNotAnInjectionPoint() {
    addModule("bind(MyService.class).to(MyServiceImpl.class);");
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import com.google.inject.assistedinject.Assisted;
      public class Client {
        @Inject
        public Client(MyService service, @Assisted MyService assisted) {}
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("service");
  }

  public void testNamedFieldDoesNotLinkToInjectConstructor() {
    myFixture.addClass("""
      public class Jit {
        @com.google.inject.Inject public Jit() {}
      }
      """);
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import com.google.inject.name.Named;
      public class Client {
        @Inject private Jit plain;
        @Inject @Named("x") private Jit named;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("plain");
  }

  public void testJavaConstructorParametersOnOwnLines() {
    addModule(PLAIN_AND_NAMED_BINDINGS);
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import com.google.inject.name.Named;
      public class Client {
        @Inject Client(
          @Named("db") MyService db,
          MyService plain) {}
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("db", "plain");
  }

  // Each parameter is on its own line: the platform merges markers of one line into one icon with another tooltip.
  public void testKotlinNamedConstructorParameter() {
    addModule(PLAIN_AND_NAMED_BINDINGS);
    myFixture.configureByText("Client.kt", """
      import com.google.inject.Inject
      import com.google.inject.name.Named
      class Client @Inject constructor(
        @Named("db") private val db: MyService,
        private val plain: MyService,
      )
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("db", "plain");
    assertThat(gutterTargets(TO_BINDINGS_TOOLTIP)).containsExactly(
      "bind(MyService.class).annotatedWith(Names.named(\"db\")).to(MyServiceImpl.class)",
      "bind(MyService.class).to(MyServiceImpl.class)");
  }
  public void testInterfaceFieldDoesNotLinkToImplementationConstructor() {
    myFixture.addClass("""
      public class InjectableServiceImpl implements MyService {
        @com.google.inject.Inject
        public InjectableServiceImpl() {}
      }
      """);
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        @Inject private MyService service;
      }
      """);

    // Guice matches keys by the exact type. A subtype with an @Inject constructor is not a binding of MyService.
    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).isEmpty();
  }

  public void testObjectFieldDoesNotLinkToEveryBinding() {
    addModule("bind(MyService.class).to(MyServiceImpl.class);");
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        @Inject private Object anything;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).isEmpty();
  }

  public void testGenericTypeArgumentsMustBeEqual() {
    addModule("""
      bind(new com.google.inject.TypeLiteral<java.util.List<String>>() {}).toInstance(null);
      """);
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import java.util.List;
      public class Client {
        @Inject
        List<String> strings;
        @Inject
        List<Integer> numbers;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("strings");
  }

  public void testGenericClassConstructorServesEveryParameterization() {
    myFixture.addClass("""
      public class Repository<T> {
        @com.google.inject.Inject
        public Repository() {}
      }
      """);
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        @Inject private Repository<String> repository;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("repository");
  }

  public void testPrimitiveMatchesWrapperBinding() {
    addModule("bind(Integer.class).annotatedWith(com.google.inject.name.Names.named(\"port\")).toInstance(8080);");
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import com.google.inject.name.Named;
      public class Client {
        @Inject @Named("port") private int port;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("port");
  }

  public void testClassWithoutConstructorIsJitBinding() {
    myFixture.addClass("public class Plain {}");
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        @Inject private Plain plain;
      }
      """);

    assertThat(gutterTargets(TO_BINDINGS_TOOLTIP)).containsExactly("public class Plain {}");
  }

  public void testNoArgConstructorIsJitBinding() {
    myFixture.addClass("public class Plain { Plain() {} Plain(String s) {} }");
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        @Inject private Plain plain;
      }
      """);

    assertThat(gutterTargets(TO_BINDINGS_TOOLTIP)).containsExactly("Plain() {}");
  }

  public void testNoJitBindingForUnconstructableClasses() {
    myFixture.addClass("public class PrivateConstructor { private PrivateConstructor() {} }");
    myFixture.addClass("public class ArgumentConstructor { public ArgumentConstructor(String s) {} }");
    myFixture.addClass("public abstract class AbstractService {}");
    myFixture.addClass("public class Outer { public class Inner {} }");
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        @Inject
        PrivateConstructor privateConstructor;
        @Inject
        ArgumentConstructor argumentConstructor;
        @Inject
        AbstractService abstractService;
        @Inject
        Outer.Inner inner;
        @Inject
        MyService service;
        @Inject
        String text;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).isEmpty();
  }

  public void testNoJitBindingForQualifiedKey() {
    myFixture.addClass("public class Plain {}");
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import com.google.inject.name.Named;
      public class Client {
        @Inject @Named("plain") private Plain plain;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).isEmpty();
  }

  public void testExplicitBindingHidesJitBinding() {
    myFixture.addClass("public class Plain {}");
    addModule("bind(Plain.class).toInstance(new Plain());");
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        @Inject private Plain plain;
      }
      """);

    assertThat(gutterTargets(TO_BINDINGS_TOOLTIP)).containsExactly("bind(Plain.class).toInstance(new Plain())");
  }

  public void testImplementedByIsBinding() {
    myFixture.addClass("public class ApiImpl implements Api {}");
    myFixture.addClass("""
      @com.google.inject.ImplementedBy(ApiImpl.class)
      public interface Api {}
      """);
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        @Inject private Api api;
      }
      """);

    assertThat(gutterTargets(TO_BINDINGS_TOOLTIP)).containsExactly("@com.google.inject.ImplementedBy(ApiImpl.class)");
  }

  public void testImplementedByInImplementationClassGutter() {
    PsiClass implClass = myFixture.addClass("public class ApiImpl implements Api {}");
    myFixture.addClass("""
      @com.google.inject.ImplementedBy(ApiImpl.class)
      public interface Api {}
      """);
    myFixture.configureFromExistingVirtualFile(implClass.getContainingFile().getVirtualFile());

    assertThat(gutterTargets(TO_BINDINGS_TOOLTIP)).containsExactly("@com.google.inject.ImplementedBy(ApiImpl.class)");
  }

  public void testImplementedByNotInOwnClassGutter() {
    myFixture.addClass("public class ApiImpl implements Api {}");
    myFixture.configureByText("Api.java", """
      @com.google.inject.ImplementedBy(ApiImpl.class)
      public interface Api {}
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).isEmpty();
  }

  public void testProvidedByIsBinding() {
    myFixture.addClass("""
      public class ApiProvider implements com.google.inject.Provider<Api> {
        public Api get() { return null; }
      }
      """);
    myFixture.addClass("""
      @com.google.inject.ProvidedBy(ApiProvider.class)
      public interface Api {}
      """);
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        @Inject private Api api;
      }
      """);

    assertThat(gutterTargets(TO_BINDINGS_TOOLTIP)).containsExactly("@com.google.inject.ProvidedBy(ApiProvider.class)");
  }

  public void testMultibinderBindsCollectionOfProviders() {
    addModule("Multibinder.newSetBinder(binder(), MyService.class).addBinding().to(MyServiceImpl.class);");
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import com.google.inject.Provider;
      import java.util.Collection;
      import java.util.Set;
      public class Client {
        @Inject
        Set<MyService> services;
        @Inject
        Collection<Provider<MyService>> providers;
        @Inject
        MyService single;
      }
      """);

    // A multibinder element does not bind MyService itself.
    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("providers", "services");
  }

  public void testMapBinderBindsMapOfProviders() {
    addModule("MapBinder.newMapBinder(binder(), MyKey.class, MyService.class).addBinding(null).to(MyServiceImpl.class);");
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import com.google.inject.Provider;
      import java.util.Map;
      public class Client {
        @Inject
        Map<MyKey, MyService> services;
        @Inject
        Map<MyKey, Provider<MyService>> providers;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("providers", "services");
  }

  public void testOptionalBinderWithDefaultBindsTheElement() {
    addModule("OptionalBinder.newOptionalBinder(binder(), MyService.class).setDefault().to(MyServiceImpl.class);");
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import com.google.inject.Provider;
      import java.util.Optional;
      public class Client {
        @Inject
        Optional<MyService> optional;
        @Inject
        Optional<Provider<MyService>> optionalProvider;
        @Inject
        MyService service;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("optional", "optionalProvider", "service");
  }

  public void testOptionalBinderWithoutValueDoesNotBindTheElement() {
    addModule("OptionalBinder.newOptionalBinder(binder(), MyService.class);");
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import java.util.Optional;
      public class Client {
        @Inject
        Optional<MyService> optional;
        @Inject
        MyService service;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("optional");
  }

  public void testProvidesIntoOptional() {
    myFixture.addFileToProject("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.multibindings.ProvidesIntoOptional;
      public class MyModule extends AbstractModule {
        @ProvidesIntoOptional(ProvidesIntoOptional.Type.DEFAULT)
        MyService provideService() { return null; }
      }
      """);
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import java.util.Optional;
      public class Client {
        @Inject
        Optional<MyService> optional;
        @Inject
        MyService service;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("optional", "service");
  }

  public void testMultibinderElementImplementationClassGutter() {
    addModule("Multibinder.newSetBinder(binder(), MyService.class).addBinding().to(ElementImpl.class);");
    myFixture.configureByText("ElementImpl.java", """
      public class ElementImpl implements MyService {}
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("ElementImpl");
  }

  public void testMultibinderConcreteElementClassGutter() {
    myFixture.addClass("public class ConcreteElement {}");
    addModule("Multibinder.newSetBinder(binder(), ConcreteElement.class).addBinding().to(ConcreteElement.class);");
    myFixture.configureByText("ConcreteElement.java", """
      public class ConcreteElement {}
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("ConcreteElement");
  }

  public void testMultibinderWithGenericElementType() {
    myFixture.addFileToProject("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.TypeLiteral;
      import com.google.inject.multibindings.Multibinder;
      import java.util.List;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          Multibinder.newSetBinder(binder(), new TypeLiteral<List<String>>() {})
              .addBinding().toInstance(List.of());
        }
      }
      """);
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import java.util.List;
      import java.util.Set;
      public class Client {
        @Inject
        Set<List<String>> stringLists;
        @Inject
        Set<List<Integer>> intLists;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("stringLists");
  }

  public void testMapBinderWithGenericValueType() {
    myFixture.addFileToProject("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.TypeLiteral;
      import com.google.inject.multibindings.MapBinder;
      import java.util.List;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          MapBinder.newMapBinder(binder(), new TypeLiteral<String>() {}, new TypeLiteral<List<String>>() {})
              .addBinding("a").toInstance(List.of());
        }
      }
      """);
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import java.util.List;
      import java.util.Map;
      public class Client {
        @Inject
        Map<String, List<String>> stringMap;
        @Inject
        Map<String, List<Integer>> intMap;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("stringMap");
  }

  public void testOptionalBinderWithGenericElementType() {
    myFixture.addFileToProject("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.TypeLiteral;
      import com.google.inject.multibindings.OptionalBinder;
      import java.util.List;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          OptionalBinder.newOptionalBinder(binder(), new TypeLiteral<List<String>>() {})
              .setDefault().toInstance(List.of());
        }
      }
      """);
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import java.util.List;
      import java.util.Optional;
      public class Client {
        @Inject
        Optional<List<String>> optionalList;
        @Inject
        List<String> directList;
        @Inject
        Optional<List<Integer>> wrongList;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("directList", "optionalList");
  }

  public void testProvidesIntoMapWithClassMapKey() {
    myFixture.addFileToProject("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.multibindings.ClassMapKey;
      import com.google.inject.multibindings.ProvidesIntoMap;
      public class MyModule extends AbstractModule {
        @ProvidesIntoMap
        @ClassMapKey(MyServiceImpl.class)
        MyService provideService() { return null; }
      }
      """);
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import java.util.Map;
      public class Client {
        @Inject
        Map<Class<?>, MyService> services;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("services");
  }

  public void testKotlinMultibinderAndMapBinderOfInterface() {
    addModule("""
      Multibinder.newSetBinder(binder(), MyService.class).addBinding().to(MyServiceImpl.class);
      MapBinder.newMapBinder(binder(), String.class, MyService.class).addBinding("a").to(MyServiceImpl.class);
      """);
    myFixture.configureByText("Client.kt", """
      import com.google.inject.Inject
      class Client {
        @Inject
        lateinit var services: Set<MyService>
        @Inject
        lateinit var serviceMap: Map<String, MyService>
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("serviceMap", "services");
  }

  public void testImplementedByHasOutgoingGuttersToInjectionPointAndImplementation() {
    myFixture.addClass("""
      public class ApiImpl implements Api {
        @com.google.inject.Inject
        public ApiImpl() {}
      }
      """);
    myFixture.addClass("""
      import com.google.inject.Inject;
      public class Client {
        @Inject
        Api api;
      }
      """);
    myFixture.configureByText("Api.java", """
      @com.google.inject.ImplementedBy(ApiImpl.class)
      public interface Api {}
      """);

    assertThat(gutterTargets(TO_INJECTION_POINTS_TOOLTIP)).containsExactly("@Inject Api api;");
    assertThat(gutterTargets(TO_BINDINGS_TOOLTIP)).containsExactly("@com.google.inject.Inject public ApiImpl() {}");
  }

  public void testExplicitBindingHidesImplementedBy() {
    myFixture.addClass("public class ApiImpl implements Api {}");
    myFixture.addClass("public class ExplicitImpl implements Api {}");
    myFixture.addClass("""
      @com.google.inject.ImplementedBy(ApiImpl.class)
      public interface Api {}
      """);
    addModule("bind(Api.class).to(ExplicitImpl.class);");
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        @Inject private Api api;
      }
      """);

    assertThat(gutterTargets(TO_BINDINGS_TOOLTIP)).containsExactly("bind(Api.class).to(ExplicitImpl.class)");
  }

  public void testExplicitBindingHidesImplementedByAndProvidedByReverseGutter() {
    myFixture.addClass("public class ApiImpl implements Api {}");
    myFixture.addClass("""
      public class ServiceProvider implements com.google.inject.Provider<Service> {
        @Override public Service get() { return null; }
      }
      """);
    myFixture.addClass("public class ExplicitImpl implements Api, Service {}");
    var apiClass = myFixture.addClass("""
      @com.google.inject.ImplementedBy(ApiImpl.class)
      public interface Api {}
      """);
    var serviceClass = myFixture.addClass("""
      @com.google.inject.ProvidedBy(ServiceProvider.class)
      public interface Service {}
      """);
    addModule("""
      bind(Api.class).to(ExplicitImpl.class);
      bind(Service.class).to(ExplicitImpl.class);
      """);
    myFixture.addClass("""
      import com.google.inject.Inject;
      public class Client {
        @Inject Api api;
        @Inject Service service;
      }
      """);

    myFixture.openFileInEditor(apiClass.getContainingFile().getVirtualFile());
    assertThat(gutterAnchors(TO_INJECTION_POINTS_TOOLTIP)).isEmpty();

    myFixture.openFileInEditor(serviceClass.getContainingFile().getVirtualFile());
    assertThat(gutterAnchors(TO_INJECTION_POINTS_TOOLTIP)).isEmpty();
  }

  public void testNoJitBindingForLocalClass() {
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        void work() {
          class Local {
            @Inject
            Local self;
          }
        }
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).isEmpty();
  }

  public void testQualifiedMultibinderMapBinderAndOptionalBinder() {
    myFixture.addFileToProject("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.Key;
      import com.google.inject.multibindings.MapBinder;
      import com.google.inject.multibindings.Multibinder;
      import com.google.inject.multibindings.OptionalBinder;
      import com.google.inject.name.Names;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          Multibinder.newSetBinder(binder(), MyService.class, Names.named("a"))
              .addBinding().to(MyServiceImpl.class);
          MapBinder.newMapBinder(binder(), String.class, MyService.class, Names.named("a"))
              .addBinding("k").to(MyServiceImpl.class);
          OptionalBinder.newOptionalBinder(binder(), Key.get(MyService.class, Names.named("a")))
              .setDefault().to(MyServiceImpl.class);
        }
      }
      """);
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import com.google.inject.name.Named;
      import java.util.Map;
      import java.util.Optional;
      import java.util.Set;
      public class Client {
        @Inject @Named("a")
        Set<MyService> qualifiedSet;
        @Inject
        Set<MyService> unqualifiedSet;
        @Inject @Named("a")
        Map<String, MyService> qualifiedMap;
        @Inject
        Map<String, MyService> unqualifiedMap;
        @Inject @Named("a")
        Optional<MyService> qualifiedOptional;
        @Inject
        Optional<MyService> unqualifiedOptional;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP))
        .containsExactly("qualifiedMap", "qualifiedOptional", "qualifiedSet");
  }

  public void testKotlinPropertyQualifierWithoutUseSiteTarget() {
    addModule("""
      bind(MyService.class).annotatedWith(com.google.inject.name.Names.named("a")).to(MyServiceImpl.class);
      bind(MyService.class).to(MyServiceImpl.class);
      """);
    myFixture.configureByText("Client.kt", """
      import javax.inject.Inject
      import javax.inject.Named
      class Client {
        @Inject
        @Named("a")
        lateinit var service: MyService
      }
      """);

    assertThat(gutterTargets(TO_BINDINGS_TOOLTIP))
        .containsExactly("bind(MyService.class).annotatedWith(com.google.inject.name.Names.named(\"a\")).to(MyServiceImpl.class)");
  }

  public void testQualifierWithDefaultAttributeDoesNotMatchDifferentAttributeValue() {
    myFixture.addClass("""
      import com.google.inject.BindingAnnotation;
      import java.lang.annotation.Retention;
      import java.lang.annotation.RetentionPolicy;
      @BindingAnnotation
      @Retention(RetentionPolicy.RUNTIME)
      public @interface Db {
        String value() default "main";
      }
      """);
    myFixture.addFileToProject("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.Provides;
      public class MyModule extends AbstractModule {
        @Provides @Db("replica")
        MyService provideReplica() { return null; }
      }
      """);
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        @Inject @Db
        MyService mainDb;
        @Inject @Db("replica")
        MyService replicaDb;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("replicaDb");
  }

  public void testBindingClassGutterUpdatesOnInMemoryEditInSameFile() {
    myFixture.configureByText("SelfModule.java", """
      import com.google.inject.AbstractModule;
      public class SelfModule extends AbstractModule {
        public static class NestedImpl implements MyService {}
        @Override
        protected void configure() {
          <caret>
        }
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).isEmpty();

    WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      myFixture.getEditor().getDocument().insertString(
          myFixture.getCaretOffset(),
          "bind(MyService.class).to(NestedImpl.class);");
    });
    PsiDocumentManager.getInstance(getProject()).commitAllDocuments();

    var elements = PsiTreeUtil.collectElementsOfType(myFixture.getFile(), PsiElement.class);
    var markers = new ArrayList<RelatedItemLineMarkerInfo<?>>();
    new GuiceBindingClassAnnotator()
        .collectNavigationMarkers(new ArrayList<>(elements), markers, false);
    assertThat(markers).isNotEmpty();
  }

  public void testReindexCurrentFileRetriesAfterCancellation() {
    var throwOnce = new AtomicBoolean(false);
    var disposable = Disposer.newDisposable();
    try {
      GuiceBindingContributor.EP_NAME.getPoint().registerExtension(
          new GuiceBindingContributor() {
            @Override
            public @NotNull java.util.Set<String> getBindingWords() {
              if (throwOnce.compareAndSet(true, false)) {
                throw new ProcessCanceledException();
              }
              return Set.of();
            }

            @Override
            public boolean processCall(@NotNull UCallExpression call,
                                       @NotNull String callName,
                                       @NotNull String containingClassFqn,
                                       @NotNull PsiClass containingClass,
                                       @NotNull Set<BindDescriptor> result) {
              return false;
            }
          },
          disposable);

      myFixture.addClass("""
        import com.google.inject.Inject;
        public class Client {
          @Inject
          MyService service;
        }
        """);
      myFixture.configureByText("MyModule.java", """
        import com.google.inject.AbstractModule;
        public class MyModule extends AbstractModule {
          @Override
          protected void configure() {
            <caret>
          }
        }
        """);
      assertThat(gutterAnchors(TO_INJECTION_POINTS_TOOLTIP)).isEmpty();

      WriteCommandAction.runWriteCommandAction(getProject(), () -> {
        myFixture.getEditor().getDocument().insertString(
            myFixture.getCaretOffset(),
            "bind(MyService.class).to(MyServiceImpl.class);");
      });
      PsiDocumentManager.getInstance(getProject()).commitAllDocuments();

      var model = GuiceProjectModel.getInstance(getProject());
      throwOnce.set(true);
      try {
        model.reindexCurrentFile(myFixture.getFile());
      }
      catch (ProcessCanceledException ignored) {
      }
      assertThat(gutterAnchors(TO_INJECTION_POINTS_TOOLTIP)).containsExactly("bind");
    }
    finally {
      Disposer.dispose(disposable);
    }
  }

  public void testGetBindingDescriptorsForSingleModuleInMultiModuleFile() {
    var psiFile = myFixture.addFileToProject("Modules.java", """
      import com.google.inject.AbstractModule;
      public class Modules {
        public static class FirstModule extends AbstractModule {
          @Override
          protected void configure() {
            bind(MyService.class).to(MyServiceImpl.class);
            install(new AbstractModule() {
              @Override
              protected void configure() {
                bind(Runnable.class).toInstance(() -> {});
              }
            });
          }
        }
        public static class SecondModule extends AbstractModule {
          @Override
          protected void configure() {
            bind(MyKey.class).toInstance(new MyKey() {});
          }
        }
      }
      """);

    var firstModule = ((PsiJavaFile)psiFile).getClasses()[0].findInnerClassByName("FirstModule", false);
    assertNotNull(firstModule);
    var descriptors = GuiceInjectorManager.getBindingDescriptors(firstModule);
    assertThat(descriptors).hasSize(1);
    assertThat(descriptors.iterator().next().getBoundClass().getName()).isEqualTo("MyService");
  }

  public void testGuiceEntryHashCodeIncludesKey() {
    var psiFile = myFixture.addFileToProject("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.multibindings.Multibinder;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          Multibinder.newSetBinder(binder(), MyService.class).addBinding().to(MyServiceImpl.class);
        }
      }
      """);

    var entries = GuiceEntryProducer.extractFromFile(psiFile);
    var bindingEntries = entries.stream()
        .filter(e -> e.getRole() == EntryRole.BINDING_SITE)
        .toList();
    assertThat(bindingEntries.size()).isGreaterThan(1);
    var distinctHashes = bindingEntries.stream().map(Object::hashCode).distinct().count();
    assertThat(distinctHashes).isEqualTo(bindingEntries.size());
  }

  public void testTargetRendererDoesNotRetainPsiOrLoseTextAfterReparse() throws Exception {
    assertThat(java.util.Arrays.stream(GuiceEntry.class.getDeclaredFields())
                   .anyMatch(f -> f.getType() == GuiceBindingContributor.class))
        .isFalse();

    var clientFile = myFixture.addFileToProject("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        @Inject
        void init(MyService svc) {}
      }
      """);
    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(MyService.class).to(MyServiceImpl.class);
        }
      }
      """);
    myFixture.doHighlighting();

    NavigationGutterIconRenderer navRenderer = null;
    for (GutterMark mark : myFixture.findAllGutters()) {
      if (TO_INJECTION_POINTS_TOOLTIP.equals(mark.getTooltipText())) {
        var info = ((LineMarkerGutterIconRenderer<?>)mark).getLineMarkerInfo();
        navRenderer = (NavigationGutterIconRenderer)info.getNavigationHandler();
        break;
      }
    }
    assertNotNull(navRenderer);

    WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      var doc = PsiDocumentManager.getInstance(getProject()).getDocument(clientFile);
      assertNotNull(doc);
      doc.insertString(0, "// comment\n");
    });
    PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
    PsiDocumentManager.getInstance(getProject()).reparseFiles(
        List.of(clientFile.getVirtualFile()), false);

    var targets = navRenderer.getTargetElements();
    assertThat(targets).hasSize(1);
    var field = NavigationGutterIconRenderer.class.getDeclaredField("myTargetRenderer");
    field.setAccessible(true);
    @SuppressWarnings("unchecked")
    var supplier = (Supplier<PsiTargetPresentationRenderer<PsiElement>>)field.get(navRenderer);
    var targetRenderer = supplier.get();
    assertThat(targetRenderer.getElementText(targets.getFirst())).isEqualTo("init(svc)");
  }

  public void testVfsRenameAndDirectoryDeleteRemoveStaleEntries() throws Exception {
    var subFile = myFixture.addFileToProject("sub/SubModule.java", """
      import com.google.inject.AbstractModule;
      public class SubModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(MyService.class).to(MyServiceImpl.class);
        }
      }
      """);
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        @Inject
        MyService service;
      }
      """);
    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("service");

    var vf = subFile.getVirtualFile();
    String oldPath = vf.getPath();
    WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      try {
        vf.rename(this, "RenamedSubModule.java");
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    assertThat(getNavigationIndex().getIndexedFiles()).doesNotContain(oldPath);

    var movedDir = myFixture.getTempDirFixture().findOrCreateDir("moved");
    String renamedPath = vf.getPath();
    WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      try {
        vf.move(this, movedDir);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    assertThat(getNavigationIndex().getIndexedFiles()).doesNotContain(renamedPath);
    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("service");

    WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      try {
        movedDir.delete(this);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).isEmpty();
  }

  public void testProvidesIntoSetAndCheckedProvides() {
    myFixture.addClass("""
      package com.google.inject.throwingproviders;
      public interface CheckedProvider<T> {
        T get() throws Exception;
      }
      """);
    myFixture.addClass("""
      package com.google.inject.throwingproviders;
      import java.lang.annotation.Retention;
      import java.lang.annotation.RetentionPolicy;
      @Retention(RetentionPolicy.RUNTIME)
      public @interface CheckedProvides {
        Class<? extends CheckedProvider> value();
      }
      """);
    myFixture.addClass("""
      import com.google.inject.throwingproviders.CheckedProvider;
      public interface ServiceCheckedProvider<T> extends CheckedProvider<T> {}
      """);
    myFixture.addFileToProject("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.multibindings.ProvidesIntoSet;
      import com.google.inject.throwingproviders.CheckedProvides;
      public class MyModule extends AbstractModule {
        @ProvidesIntoSet
        MyService provideIntoSet() { return null; }

        @CheckedProvides(ServiceCheckedProvider.class)
        MyService provideChecked() { return null; }
      }
      """);
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import java.util.Set;
      public class Client {
        @Inject
        Set<MyService> serviceSet;
        @Inject
        ServiceCheckedProvider<MyService> checkedService;
        @Inject
        ServiceCheckedProvider<Runnable> wrongChecked;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("checkedService", "serviceSet");
  }

  public void testMultimapBinderWithGenericValueType() {
    myFixture.addClass("""
      package com.google.common.collect;
      public interface Multimap<K, V> {}
      """);
    myFixture.addClass("""
      package com.google.common.inject;
      import com.google.inject.Binder;
      import com.google.inject.TypeLiteral;
      import com.google.inject.binder.LinkedBindingBuilder;
      public abstract class MultimapBinder<K, V> {
        public static <K, V> MultimapBinder<K, V> newSetMultimapBinder(
            Binder binder, TypeLiteral<K> keyType, TypeLiteral<V> valueType) {
          return null;
        }
        public abstract LinkedBindingBuilder<V> addBinding(K key);
      }
      """);
    myFixture.addFileToProject("MyModule.java", """
      import com.google.common.inject.MultimapBinder;
      import com.google.inject.AbstractModule;
      import com.google.inject.TypeLiteral;
      import java.util.List;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          MultimapBinder.newSetMultimapBinder(
              binder(), new TypeLiteral<String>() {}, new TypeLiteral<List<String>>() {})
              .addBinding("k").toInstance(List.of());
        }
      }
      """);
    myFixture.configureByText("Client.java", """
      import com.google.common.collect.Multimap;
      import com.google.inject.Inject;
      import java.util.List;
      public class Client {
        @Inject
        Multimap<String, List<String>> stringMultimap;
        @Inject
        Multimap<String, List<Integer>> intMultimap;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("stringMultimap");
  }

  public void testAssistedFactoryWithGenericFactoryType() {
    myFixture.addClass("""
      public interface GenericFactory<T> {
        T create(String name);
      }
      """);
    myFixture.addClass("""
      import com.google.inject.Inject;
      import com.google.inject.assistedinject.Assisted;
      public class AssistedService implements MyService {
        @Inject
        public AssistedService(@Assisted String name) {}
      }
      """);
    myFixture.addFileToProject("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.TypeLiteral;
      import com.google.inject.assistedinject.FactoryModuleBuilder;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          install(new FactoryModuleBuilder()
              .implement(MyService.class, AssistedService.class)
              .build(new TypeLiteral<GenericFactory<MyService>>() {}));
        }
      }
      """);
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        @Inject
        GenericFactory<MyService> serviceFactory;
        @Inject
        GenericFactory<Runnable> runnableFactory;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("serviceFactory");
  }

  public void testPrimitiveClassBindingAndBindConstant() {
    myFixture.addFileToProject("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.name.Names;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(int.class).annotatedWith(Names.named("maxRetries")).toInstance(3);
          bindConstant().annotatedWith(Names.named("port")).to(8080);
          bindConstant().annotatedWith(Names.named("host")).to("localhost");
        }
      }
      """);
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import com.google.inject.name.Named;
      public class Client {
        @Inject @Named("maxRetries")
        int maxRetries;
        @Inject @Named("port")
        int port;
        @Inject @Named("host")
        String host;
        @Inject @Named("other")
        int other;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("host", "maxRetries", "port");
  }

  public void testAssistedFactoryBuildWithKeyQualifier() {
    myFixture.addClass("""
      public interface MyFactory {
        MyService create(String name);
      }
      """);
    myFixture.addFileToProject("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.Key;
      import com.google.inject.assistedinject.FactoryModuleBuilder;
      import com.google.inject.name.Names;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          install(new FactoryModuleBuilder()
              .implement(MyService.class, MyServiceImpl.class)
              .build(Key.get(MyFactory.class, Names.named("special"))));
        }
      }
      """);
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import com.google.inject.name.Named;
      public class Client {
        @Inject @Named("special")
        MyFactory specialFactory;
        @Inject
        MyFactory unqualifiedFactory;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("specialFactory");
  }

  public void testStandardBindTextNormalizesWhitespaceAndTruncatesLongArguments() {
    var psiFile = myFixture.addFileToProject("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(String.class).toInstance(
              "a_very_long_string_argument_that_exceeds_the_maximum_display_length_for_popup_presentation");
        }
      }
      """);

    var entries = GuiceEntryProducer.extractFromFile(psiFile);
    var bindingEntry = entries.stream()
        .filter(e -> e.getRole() == EntryRole.BINDING_SITE)
        .findFirst()
        .orElseThrow();
    String text = bindingEntry.getPresentableText();
    assertThat(text).doesNotContain("\n");
    assertThat(text).contains("...");
    assertThat(text.length()).isAtMost(90);
  }

  public void testMapBinderPermitDuplicatesAndOptionalBinderLocalVariable() {
    myFixture.addFileToProject("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.multibindings.MapBinder;
      import com.google.inject.multibindings.OptionalBinder;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          MapBinder.newMapBinder(binder(), String.class, MyService.class).permitDuplicates();
          MapBinder.newMapBinder(binder(), Integer.class, MyService.class);
          OptionalBinder<MyService> ob = OptionalBinder.newOptionalBinder(binder(), MyService.class);
          ob.setDefault().to(MyServiceImpl.class);
        }
      }
      """);
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import com.google.inject.Provider;
      import java.util.Collection;
      import java.util.Map;
      import java.util.Set;
      public class Client {
        @Inject
        Map<String, Set<MyService>> mapOfSets;
        @Inject
        Map<String, Set<Provider<MyService>>> mapOfProviderSets;
        @Inject
        Map<String, Collection<Provider<MyService>>> mapOfProviderCollections;
        @Inject
        Map<Integer, Set<MyService>> nonDuplicateMapOfSets;
        @Inject
        MyService directFromLocalOptionalBinder;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly(
        "directFromLocalOptionalBinder",
        "mapOfProviderCollections",
        "mapOfProviderSets",
        "mapOfSets");
  }

  public void testSetSubtypesAndSuperWildcardsDoNotMatchMultibinder() {
    myFixture.addFileToProject("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.multibindings.Multibinder;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          Multibinder.newSetBinder(binder(), MyService.class).addBinding().to(MyServiceImpl.class);
        }
      }
      """);
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import java.util.HashSet;
      import java.util.Set;
      public class Client {
        @Inject
        Set<MyService> exactSet;
        @Inject
        HashSet<MyService> hashSet;
        @Inject
        Set<? super MyService> superWildcardSet;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("exactSet");
  }

  public void testIntentionPredicates() {
    var psiFile = myFixture.addFileToProject("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.Inject;
      import com.google.inject.Scopes;
      public class MyModule extends AbstractModule {
        @Inject
        MyService injectedField;

        @Override
        protected void configure() {
          bind(MyService.class).to(MyServiceImpl.class).in(Scopes.SINGLETON);
          bind(MyService.class).toProvider(MyServiceProvider.class);
        }
      }
      """);

    var field = ((com.intellij.psi.PsiJavaFile)psiFile).getClasses()[0].getFields()[0];
    var annotation = field.getAnnotations()[0];
    assertTrue(new com.intellij.guice.intentions.ToggleInjectionRequiredPredicate().satisfiedBy(annotation));

    var configureBody = ((com.intellij.psi.PsiJavaFile)psiFile).getClasses()[0].getMethods()[0].getBody();
    var firstStmtExpr = ((com.intellij.psi.PsiExpressionStatement)configureBody.getStatements()[0]).getExpression();
    var secondStmtExpr = ((com.intellij.psi.PsiExpressionStatement)configureBody.getStatements()[1]).getExpression();

    assertTrue(new com.intellij.guice.intentions.MoveBindingToClassPredicate().satisfiedBy(firstStmtExpr));
    assertTrue(new com.intellij.guice.intentions.MoveBindingScopeToClassPredicate().satisfiedBy(firstStmtExpr));
    assertFalse(new com.intellij.guice.intentions.MoveProviderBindingToClassPredicate().satisfiedBy(firstStmtExpr));

    assertTrue(new com.intellij.guice.intentions.MoveProviderBindingToClassPredicate().satisfiedBy(secondStmtExpr));
    assertFalse(new com.intellij.guice.intentions.MoveBindingScopeToClassPredicate().satisfiedBy(secondStmtExpr));
  }

  public void testDeclarativeFlagSpecAndCustomInjectionPointContributor() {
    myFixture.addClass("""
      package com.google.common.flags;
      import java.lang.annotation.Retention;
      import java.lang.annotation.RetentionPolicy;
      @Retention(RetentionPolicy.RUNTIME)
      public @interface FlagSpec {
        String name();
        String help() default "";
      }
      """);
    myFixture.addClass("""
      package com.google.common.flags;
      public final class Flag<T> {
        public static <T> Flag<T> value(T defaultValue) { return new Flag<>(); }
      }
      """);
    myFixture.addClass("""
      package com.google.common.flags.ext.guice;
      import com.google.inject.Module;
      public final class FlagBinder {
        public static Module createModule(Class<?>... flagContainers) { return null; }
      }
      """);
    myFixture.addClass("""
      import com.google.inject.BindingAnnotation;
      import java.lang.annotation.Retention;
      import java.lang.annotation.RetentionPolicy;
      @Retention(RetentionPolicy.RUNTIME)
      @BindingAnnotation
      public @interface GmailifyOpenAuthRedirectUrl {}
      """);
    myFixture.addClass("""
      public final class CustomInjector {
        public <T> T lookup(Class<T> type) { return null; }
      }
      """);

    GuiceBindingContributor flagAndCustomLookupContributor = new GuiceBindingContributor() {
      @Override
      public void register(@NotNull GuiceExtensionRegistrar registrar) {
        registrar.registerFieldAnnotation(
            List.of("com.google.common.flags.FlagSpec"),
            (field, entries) -> {
              if (field.getType() instanceof com.intellij.psi.PsiClassType classType) {
                var params = classType.getParameters();
                if (params.length == 1) {
                  entries.add(GuiceEntryProducer.createFieldBindingEntry(field, params[0]));
                }
              }
            }
        );
        registrar.registerModuleCallEntry(
            GuiceCallPattern.named("createModule")
                .withOwnerClass("com.google.common.flags.ext.guice.FlagBinder")
                .minArguments(1),
            (call, entries) -> {
              var argType = GuiceUtils.getBindingTypeFromExpression(call.getValueArguments().getFirst());
              if (argType != null) {
                var entry = GuiceEntryProducer.createCallEntry(call, argType, null, EntryRole.INJECTION_POINT);
                if (entry != null) entries.add(entry);
              }
            }
        );
        registrar.registerCallEntry(
            GuiceCallPattern.named("lookup")
                .withOwnerClass("CustomInjector")
                .argumentCount(1),
            (call, entries) -> {
              var argType = GuiceUtils.getBindingTypeFromExpression(call.getValueArguments().getFirst());
              if (argType != null) {
                var entry = GuiceEntryProducer.createCallEntry(call, argType, null, EntryRole.INJECTION_POINT);
                if (entry != null) entries.add(entry);
              }
            }
        );
      }
    };
    GuiceBindingContributor.EP_NAME.getPoint().registerExtension(
        flagAndCustomLookupContributor, getTestRootDisposable());

    var flagsFile = myFixture.addFileToProject("GmailifyFlags.java", """
      import com.google.common.flags.Flag;
      import com.google.common.flags.FlagSpec;
      public class GmailifyFlags {
        @FlagSpec(
            help = "Redirect URL that should be used for Gmailify when interacting with OpenAuth",
            name = "gmailify_open_auth_redirect_url")
        @GmailifyOpenAuthRedirectUrl
        private static final Flag<String> gmailifyOpenAuthRedirectUrl =
            Flag.value("https://mail.google.com/mail/gmat/");
      }
      """);
    myFixture.addFileToProject("FlagsModule.java", """
      import com.google.common.flags.ext.guice.FlagBinder;
      import com.google.inject.AbstractModule;
      public class FlagsModule extends AbstractModule {
        @Override
        protected void configure() {
          install(FlagBinder.createModule(GmailifyFlags.class));
          bind(MyService.class).to(MyServiceImpl.class);
        }
      }
      """);

    myFixture.enableInspections(new com.intellij.guice.inspections.BindingAnnotationWithoutInjectInspection());
    myFixture.configureFromExistingVirtualFile(flagsFile.getVirtualFile());
    assertThat(myFixture.doHighlighting(com.intellij.lang.annotation.HighlightSeverity.WARNING)).isEmpty();
    assertThat(gutterAnchors(TO_INJECTION_POINTS_TOOLTIP)).isEmpty();

    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        @Inject @GmailifyOpenAuthRedirectUrl
        String redirectUrl;

        void useCustomLookup(CustomInjector injector) {
          injector.lookup(MyService.class);
        }
      }
      """);
    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("lookup", "redirectUrl");

    myFixture.configureFromExistingVirtualFile(flagsFile.getVirtualFile());
    assertThat(gutterAnchors(TO_INJECTION_POINTS_TOOLTIP)).containsExactly("gmailifyOpenAuthRedirectUrl");
  }

  public void testFlagBinderBuiltInContributor() {
    myFixture.addClass("""
      package com.google.common.flags;
      import java.lang.annotation.Retention;
      import java.lang.annotation.RetentionPolicy;
      @Retention(RetentionPolicy.RUNTIME)
      public @interface FlagSpec {
        String name();
        String altName() default "";
        String help() default "";
      }
      """);
    myFixture.addClass("""
      package com.google.common.flags;
      public class Flag<T> {
        public static <T> Flag<T> value(T defaultValue) { return new Flag<>(); }
      }
      """);
    myFixture.addClass("""
      package com.google.common.flags;
      public final class StringFlag extends Flag<String> {}
      """);
    myFixture.addClass("""
      package com.google.common.inject;
      import com.google.inject.Binder;
      import com.google.inject.Module;
      public final class FlagBinder {
        public FlagBinder(Binder binder) {}
        public static Module createModule(Class<?>... classes) { return null; }
        public static Module legacyCreateModuleForBindingAnnotationsOrNameAndAltName(Class<?>... classes) { return null; }
        public FlagBinder legacyForBindingAnnotationsOrNameAndAltName() { return this; }
        public FlagBinder legacyForBindingAnnotationsOrName() { return this; }
        public FlagBinder legacyForBindingAnnotationsOrAltName() { return this; }
        public FlagBinder bind(Class<?>... classes) { return this; }
      }
      """);
    myFixture.addClass("""
      import com.google.inject.BindingAnnotation;
      import java.lang.annotation.Retention;
      import java.lang.annotation.RetentionPolicy;
      @Retention(RetentionPolicy.RUNTIME)
      @BindingAnnotation
      public @interface GmailifyOpenAuthRedirectUrl {}
      """);

    var flagsFile = myFixture.addFileToProject("GmailifyFlags.java", """
      import com.google.common.flags.Flag;
      import com.google.common.flags.FlagSpec;
      import com.google.common.flags.StringFlag;
      public class GmailifyFlags {
        @FlagSpec(
            help = "Redirect URL that should be used for Gmailify when interacting with OpenAuth",
            name = "gmailify_open_auth_redirect_url")
        @GmailifyOpenAuthRedirectUrl
        private static final Flag<String> gmailifyOpenAuthRedirectUrl =
            Flag.value("https://mail.google.com/mail/gmat/");

        @FlagSpec(name = "timeout_seconds", altName = "legacy_timeout_seconds", help = "Timeout")
        private static final Flag<Integer> timeoutSeconds = Flag.value(30);

        @FlagSpec(name = "server_host", help = "Host")
        private static final StringFlag serverHost = new StringFlag();
      }
      """);
    var flagsModuleFile = myFixture.addFileToProject("FlagsModule.java", """
      import com.google.common.inject.FlagBinder;
      import com.google.inject.AbstractModule;
      public class FlagsModule extends AbstractModule {
        @Override
        protected void configure() {
          install(FlagBinder.createModule(GmailifyFlags.class));
          new FlagBinder(binder()).legacyForBindingAnnotationsOrAltName().bind(GmailifyFlags.class);
        }
      }
      """);

    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import com.google.inject.name.Named;
      public class Client {
        @Inject @GmailifyOpenAuthRedirectUrl
        String redirectUrl;

        @Inject @Named("timeout_seconds")
        Integer timeoutByPrimaryName;

        @Inject @Named("legacy_timeout_seconds")
        int timeoutByAltName;

        @Inject @Named("server_host")
        String host;

        @Inject @Named("non_existent_flag")
        String missingFlag;
      }
      """);

    // Under createModule (NONE) + legacyForBindingAnnotationsOrAltName (ALT),
    // timeoutSeconds (which has altName = "legacy_timeout_seconds") is bound ONLY by its altName,
    // while serverHost (no altName) is bound by its primary name.
    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP))
        .containsExactly("host", "redirectUrl", "timeoutByAltName");

    myFixture.configureFromExistingVirtualFile(flagsFile.getVirtualFile());
    assertThat(gutterAnchors(TO_INJECTION_POINTS_TOOLTIP))
        .containsExactly("gmailifyOpenAuthRedirectUrl", "serverHost", "timeoutSeconds");
    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP))
        .containsExactly("GmailifyFlags");

    myFixture.configureFromExistingVirtualFile(flagsModuleFile.getVirtualFile());
    assertThat(gutterAnchors(TO_INJECTION_POINTS_TOOLTIP)).isEmpty();
  }

  public void testFlagBinderCrossFileEditsMovesAndDeletions() {
    myFixture.addClass("""
      package com.google.common.flags;
      import java.lang.annotation.Retention;
      import java.lang.annotation.RetentionPolicy;
      @Retention(RetentionPolicy.RUNTIME)
      public @interface FlagSpec {
        String name();
        String altName() default "";
        String help() default "";
      }
      """);
    myFixture.addClass("""
      package com.google.common.flags;
      public class Flag<T> {
        public static <T> Flag<T> value(T defaultValue) { return new Flag<>(); }
      }
      """);
    myFixture.addClass("""
      package com.google.common.inject;
      import com.google.inject.Binder;
      import com.google.inject.Module;
      public final class FlagBinder {
        public FlagBinder(Binder binder) {}
        public static Module createModule(Class<?>... classes) { return null; }
        public static Module legacyCreateModuleForBindingAnnotationsOrNameAndAltName(Class<?>... classes) { return null; }
        public FlagBinder legacyForBindingAnnotationsOrNameAndAltName() { return this; }
        public FlagBinder bind(Class<?>... classes) { return this; }
      }
      """);

    // 1. An unbound @FlagSpec class should not contribute any Guice bindings on its own.
    myFixture.addFileToProject("UnboundFlags.java", """
      import com.google.common.flags.Flag;
      import com.google.common.flags.FlagSpec;
      public class UnboundFlags {
        @FlagSpec(name = "unbound_flag", help = "Unbound")
        private static final Flag<String> unboundFlag = Flag.value("x");
      }
      """);

    var flagsFile = myFixture.addFileToProject("EditableFlags.java", """
      import com.google.common.flags.Flag;
      import com.google.common.flags.FlagSpec;
      public class EditableFlags {
        @FlagSpec(name = "endpoint_url", altName = "legacy_endpoint_url", help = "Endpoint")
        private static final Flag<String> endpointUrl = Flag.value("https://example.com");
      }
      """);
    myFixture.addFileToProject("FlagsModule.java", """
      import com.google.common.inject.FlagBinder;
      import com.google.inject.AbstractModule;
      public class FlagsModule extends AbstractModule {
        @Override
        protected void configure() {
          install(FlagBinder.legacyCreateModuleForBindingAnnotationsOrNameAndAltName(EditableFlags.class));
        }
      }
      """);

    var clientFile = myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      import com.google.inject.name.Named;
      public class Client {
        @Inject @Named("unbound_flag")
        String unbound;

        @Inject @Named("endpoint_url")
        String primaryEndpoint;

        @Inject @Named("legacy_endpoint_url")
        String altEndpoint;

        @Inject @Named("renamed_endpoint_url")
        String renamedEndpoint;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP))
        .containsExactly("altEndpoint", "primaryEndpoint");

    // 2. Edit the flag file in-place: change the flag name from "endpoint_url" to "renamed_endpoint_url".
    com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      var document = com.intellij.psi.PsiDocumentManager.getInstance(getProject()).getDocument(flagsFile);
      assertNotNull(document);
      String updated = document.getText().replace("\"endpoint_url\"", "\"renamed_endpoint_url\"");
      document.setText(updated);
      com.intellij.psi.PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
    });

    myFixture.configureFromExistingVirtualFile(clientFile.getVirtualFile());
    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP))
        .containsExactly("altEndpoint", "renamedEndpoint");

    // 3. Delete the flag file: dependent module bindings should be invalidated immediately.
    com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      try {
        flagsFile.getVirtualFile().delete(this);
      }
      catch (java.io.IOException e) {
        throw new RuntimeException(e);
      }
    });

    myFixture.configureFromExistingVirtualFile(clientFile.getVirtualFile());
    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).isEmpty();

    // 4. Re-create the flag file (simulating a move/restore): dependent module should re-bind automatically.
    myFixture.addFileToProject("EditableFlags.java", """
      import com.google.common.flags.Flag;
      import com.google.common.flags.FlagSpec;
      public class EditableFlags {
        @FlagSpec(name = "endpoint_url", help = "Restored endpoint")
        private static final Flag<String> endpointUrl = Flag.value("https://example.com");
      }
      """);

    myFixture.configureFromExistingVirtualFile(clientFile.getVirtualFile());
    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP))
        .containsExactly("primaryEndpoint");
  }

  public void testDynamicFlagBinderBuiltInContributor() {
    myFixture.addClass("""
      package com.google.common.flags;
      import java.lang.annotation.Retention;
      import java.lang.annotation.RetentionPolicy;
      @Retention(RetentionPolicy.RUNTIME)
      public @interface FlagSpec {
        String name();
        String altName() default "";
        String help() default "";
      }
      """);
    myFixture.addClass("""
      package com.google.common.flags;
      public class Flag<T> {
        public static <T> Flag<T> value(T defaultValue) { return new Flag<>(); }
      }
      """);
    myFixture.addClass("""
      package com.google.api.server.core;
      import java.lang.annotation.Retention;
      import java.lang.annotation.RetentionPolicy;
      @Retention(RetentionPolicy.RUNTIME)
      public @interface DynamicFlag {}
      """);
    myFixture.addClass("""
      package com.google.api.server.core;
      public final class DynamicFlagManager {
        public static class DynamicFlag<T> {}
      }
      """);
    myFixture.addClass("""
      package com.google.api.server.core;
      import com.google.inject.Binder;
      import com.google.inject.Module;
      public final class DynamicFlagBinder {
        public static Module dynamicFlagsModule(Class<?>... classes) { return null; }
        public static void bind(Binder binder, Class<?>... classes) {}
      }
      """);
    myFixture.addClass("""
      import com.google.inject.BindingAnnotation;
      import java.lang.annotation.Retention;
      import java.lang.annotation.RetentionPolicy;
      @Retention(RetentionPolicy.RUNTIME)
      @BindingAnnotation
      public @interface MaxQpsFlag {}
      """);

    var flagsFile = myFixture.addFileToProject("ServerDynamicFlags.java", """
      import com.google.api.server.core.DynamicFlag;
      import com.google.common.flags.Flag;
      import com.google.common.flags.FlagSpec;
      public class ServerDynamicFlags {
        @DynamicFlag
        @FlagSpec(name = "max_qps", altName = "old_max_qps", help = "Max QPS")
        @MaxQpsFlag
        public static final Flag<Integer> maxQps = Flag.value(100);
      }
      """);
    myFixture.addFileToProject("DynamicFlagsModule.java", """
      import com.google.api.server.core.DynamicFlagBinder;
      import com.google.inject.AbstractModule;
      public class DynamicFlagsModule extends AbstractModule {
        @Override
        protected void configure() {
          install(DynamicFlagBinder.dynamicFlagsModule(ServerDynamicFlags.class));
        }
      }
      """);

    myFixture.configureByText("Client.java", """
      import com.google.api.server.core.DynamicFlagManager;
      import com.google.inject.Inject;
      import com.google.inject.Provider;
      import com.google.inject.name.Named;
      public class Client {
        @Inject @Named("max_qps")
        Provider<Integer> byName;

        @Inject @Named("old_max_qps")
        Integer byAltName;

        @Inject @MaxQpsFlag
        Integer byAnnotation;

        @Inject @Named("max_qps")
        DynamicFlagManager.DynamicFlag<Integer> dynamicHandle;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP))
        .containsExactly("byAltName", "byAnnotation", "byName", "dynamicHandle");

    myFixture.configureFromExistingVirtualFile(flagsFile.getVirtualFile());
    assertThat(gutterAnchors(TO_INJECTION_POINTS_TOOLTIP)).containsExactly("maxQps");
    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("ServerDynamicFlags");
  }

  public void testExperimentFlagModuleAndExperimentValuesBuiltInContributor() {
    myFixture.addClass("""
      package com.google.experiments.framework;
      import java.lang.annotation.Retention;
      import java.lang.annotation.RetentionPolicy;
      @Retention(RetentionPolicy.RUNTIME)
      public @interface ExperimentFlagSpec {
        String name();
      }
      """);
    myFixture.addClass("""
      package com.google.experiments.framework;
      public interface ExperimentFlag<T> {}
      """);
    myFixture.addClass("""
      package com.google.apps.framework.annotations;
      import com.google.inject.BindingAnnotation;
      import java.lang.annotation.Retention;
      import java.lang.annotation.RetentionPolicy;
      @Retention(RetentionPolicy.RUNTIME)
      @BindingAnnotation
      public @interface ExperimentValue {
        String name();
      }
      """);
    myFixture.addClass("""
      package com.google.apps.framework.annotations;
      public final class ExperimentValues {
        public static ExperimentValue value(String name) { return null; }
      }
      """);
    myFixture.addClass("""
      package com.google.apps.framework.annotations;
      import com.google.inject.BindingAnnotation;
      import java.lang.annotation.Retention;
      import java.lang.annotation.RetentionPolicy;
      @Retention(RetentionPolicy.RUNTIME)
      @BindingAnnotation
      public @interface JsExperiment {}
      """);
    myFixture.addClass("""
      package com.google.apps.framework.experiments;
      import com.google.inject.AbstractModule;
      public final class ExperimentFlagModule extends AbstractModule {
        public ExperimentFlagModule(Class<?> flagContainer) {}
        public static Builder builder() { return new Builder(); }
        @Override protected void configure() {}
        public static final class Builder {
          public Builder setFlagContainer(Class<?> flagContainer) { return this; }
          public ExperimentFlagModule build() { return null; }
        }
      }
      """);
    myFixture.addClass("""
      import com.google.inject.BindingAnnotation;
      import java.lang.annotation.Retention;
      import java.lang.annotation.RetentionPolicy;
      @Retention(RetentionPolicy.RUNTIME)
      @BindingAnnotation
      public @interface EnableDarkTheme {}
      """);

    var expFlagsFile = myFixture.addFileToProject("UiExperiments.java", """
      import com.google.apps.framework.annotations.JsExperiment;
      import com.google.experiments.framework.ExperimentFlag;
      import com.google.experiments.framework.ExperimentFlagSpec;
      public class UiExperiments {
        @JsExperiment
        @ExperimentFlagSpec(name = "enable_new_sidebar")
        public static final ExperimentFlag<Boolean> enableNewSidebar = null;

        @EnableDarkTheme
        @ExperimentFlagSpec(name = "enable_dark_theme")
        public static final ExperimentFlag<Boolean> enableDarkTheme = null;
      }
      """);
    myFixture.addFileToProject("ExperimentsModule.java", """
      import com.google.apps.framework.annotations.ExperimentValues;
      import com.google.apps.framework.experiments.ExperimentFlagModule;
      import com.google.inject.AbstractModule;
      public class ExperimentsModule extends AbstractModule {
        @Override
        protected void configure() {
          install(ExperimentFlagModule.builder().setFlagContainer(UiExperiments.class).build());
          bind(String.class).annotatedWith(ExperimentValues.value("manual_exp")).toInstance("v1");
        }
      }
      """);

    myFixture.configureByText("Client.java", """
      import com.google.apps.framework.annotations.ExperimentValue;
      import com.google.inject.Inject;
      public class Client {
        @Inject @ExperimentValue(name = "enable_new_sidebar")
        Boolean newSidebar;

        @Inject @ExperimentValue(name = "other_experiment")
        Boolean otherExperiment;

        @Inject @EnableDarkTheme
        boolean darkTheme;

        @Inject @ExperimentValue(name = "manual_exp")
        String manualExperiment;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP))
        .containsExactly("darkTheme", "manualExperiment", "newSidebar");

    myFixture.configureFromExistingVirtualFile(expFlagsFile.getVirtualFile());
    assertThat(gutterAnchors(TO_INJECTION_POINTS_TOOLTIP))
        .containsExactly("enableDarkTheme", "enableNewSidebar");
    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP))
        .containsExactly("UiExperiments");
  }
}


