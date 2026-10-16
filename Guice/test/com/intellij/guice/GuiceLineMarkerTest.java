package com.intellij.guice;

import static com.google.common.truth.Truth.assertThat;

import com.intellij.codeInsight.daemon.GutterMark;
import java.util.List;
import java.util.Objects;

public class GuiceLineMarkerTest extends GuiceTestBase {

  public void testKotlinConstructorGutter() {
    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
        }
      }
      """);

    var clientFile = myFixture.configureByText("Client.kt", """
      import com.google.inject.Inject
      class Client @Inject constructor(private val service: MyService)
      """);

    myFixture.configureByText("AnotherClass.java", """
      import com.google.inject.Inject;
      public class AnotherClass {
        @Inject private Client client;
      }
      """);

    myFixture.openFileInEditor(clientFile.getVirtualFile());

    myFixture.doHighlighting();
    var gutters = myFixture.findAllGutters();

    List<String> tooltips = gutters.stream()
        .map(GutterMark::getTooltipText)
        .filter(Objects::nonNull)
        .toList();

    assertThat(tooltips).contains("Navigate to injection points");
  }

  public void testJavaConstructorGutter() {
    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
        }
      }
      """);

    var clientFile = myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        private MyService service;
        @Inject
        public Client(MyService service) {
          this.service = service;
        }
      }
      """);

    myFixture.configureByText("AnotherClass.java", """
      import com.google.inject.Inject;
      public class AnotherClass {
        @Inject private Client client;
      }
      """);

    myFixture.openFileInEditor(clientFile.getVirtualFile());
    myFixture.doHighlighting();
    var gutters = myFixture.findAllGutters();

    List<String> tooltips = gutters.stream()
        .map(GutterMark::getTooltipText)
        .filter(Objects::nonNull)
        .toList();

    assertThat(tooltips).contains("Navigate to injection points");
  }

  public void testImplementationClassGutter() {
    // The index does not re-extract a module when a class that it references appears later.
    // So the test creates the implementation class first.
    var implFile = myFixture.configureByText("FooImpl.java", """
      public class FooImpl implements MyService {}
      """);
    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(MyService.class).to(FooImpl.class);
        }
      }
      """);
    myFixture.openFileInEditor(implFile.getVirtualFile());

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("FooImpl");
    assertThat(gutterTargets(TO_BINDINGS_TOOLTIP)).containsExactly("bind(MyService.class).to(FooImpl.class)");
  }

  public void testGetProviderGutter() {
    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.Provides;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          javax.inject.Provider<MyService> provider =
            getProvider(MyService.class);
        }
        @Provides
        MyService provideService() { return null; }
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("getProvider");
    assertThat(gutterAnchors(TO_INJECTION_POINTS_TOOLTIP)).containsExactly("provideService");
    assertThat(gutterTargets(TO_INJECTION_POINTS_TOOLTIP)).containsExactly("getProvider(MyService.class)");
  }

  public void testGetProviderWithQualifiedKeyGutter() {
    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.Key;
      import com.google.inject.Provides;
      import com.google.inject.name.Named;
      import com.google.inject.name.Names;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          javax.inject.Provider<MyService> provider =
            getProvider(Key.get(MyService.class, Names.named("a")));
        }
        @Provides @Named("a")
        MyService provideA() { return null; }
        @Provides @Named("b")
        MyService provideB() { return null; }
      }
      """);

    assertThat(gutterAnchors(TO_INJECTION_POINTS_TOOLTIP)).containsExactly("provideA");
    assertThat(gutterTargets(TO_INJECTION_POINTS_TOOLTIP))
      .containsExactly("getProvider(Key.get(MyService.class, Names.named(\"a\")))");
  }
  public void testAnonymousModuleGutters() {
    myFixture.configureByText("Main.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.Provides;
      public class Main {
        Object module() {
          return new AbstractModule() {
            @Override
            protected void configure() {
              bind(MyService.class).to(MyServiceImpl.class);
            }
            @Provides
            MyKey provideKey() { return null; }
          };
        }
      }
      """);
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        @Inject
        MyService service;
        @Inject
        MyKey key;
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("key", "service");
    assertThat(gutterTargets(TO_BINDINGS_TOOLTIP)).contains("bind(MyService.class).to(MyServiceImpl.class)");
  }

  public void testToProviderClassGutterAndToProviderCallGutter() {
    var providerFile = myFixture.configureByText("CustomProvider.java", """
      public class CustomProvider implements com.google.inject.Provider<MyService> {
        @com.google.inject.Inject
        public CustomProvider() {}
        public MyService get() { return null; }
      }
      """);
    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(MyService.class).toProvider(CustomProvider.class);
        }
      }
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("toProvider");
    assertThat(gutterTargets(TO_BINDINGS_TOOLTIP)).containsExactly("@com.google.inject.Inject public CustomProvider() {}");

    myFixture.openFileInEditor(providerFile.getVirtualFile());
    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("CustomProvider");
    assertThat(gutterTargets(TO_BINDINGS_TOOLTIP)).containsExactly("bind(MyService.class).toProvider(CustomProvider.class)");
  }
}
