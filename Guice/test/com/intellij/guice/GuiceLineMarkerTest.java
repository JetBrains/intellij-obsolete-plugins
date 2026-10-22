package com.intellij.guice;

import static com.google.common.truth.Truth.assertThat;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.GutterIconDescriptor;
import com.intellij.codeInsight.daemon.GutterMark;
import com.intellij.codeInsight.daemon.LineMarkerSettings;
import com.intellij.guice.model.GuiceBindingClassAnnotator;
import com.intellij.guice.model.GuiceEntry;
import com.intellij.guice.model.GuiceEntryProducer;
import com.intellij.guice.model.GuiceInjectionsClassAnnotator;
import com.intellij.guice.model.renderers.GuiceEntryTargetRenderer;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.TestSourcesFilter;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.backend.presentation.TargetPresentation;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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

  public void testGutterIconsSettingsRegistration() {
    var bindingAnnotator = new GuiceBindingClassAnnotator();
    assertThat(bindingAnnotator.getId()).isEqualTo("guice.bound.class");
    assertThat(bindingAnnotator.getName()).isEqualTo("Bound class");
    assertThat(bindingAnnotator.getIcon()).isNotNull();

    var injectionsAnnotator = new GuiceInjectionsClassAnnotator();
    assertThat(injectionsAnnotator.getName()).isEqualTo("Guice injections and bindings");
    GutterIconDescriptor.Option[] options = injectionsAnnotator.getOptions();
    assertThat(Arrays.stream(options).map(GutterIconDescriptor.Option::getId).toList())
        .containsExactly("guice.injection.point", "guice.binding.site");

    var moduleFile = myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.Inject;
      public class MyModule extends AbstractModule {
        @Inject MyService injectedService;
        @Override
        protected void configure() {
          bind(MyService.class).to(MyServiceImpl.class);
        }
      }
      """);
    myFixture.openFileInEditor(moduleFile.getVirtualFile());

    LineMarkerSettings settings = LineMarkerSettings.getSettings();
    try {
      settings.setEnabled(options[0], false);
      DaemonCodeAnalyzer.getInstance(getProject()).restart("test");
      assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).isEmpty();
      assertThat(gutterAnchors(TO_INJECTION_POINTS_TOOLTIP)).containsExactly("bind");

      settings.setEnabled(options[0], true);
      settings.setEnabled(options[1], false);
      DaemonCodeAnalyzer.getInstance(getProject()).restart("test");
      assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("injectedService");
      assertThat(gutterAnchors(TO_INJECTION_POINTS_TOOLTIP)).isEmpty();
    }
    finally {
      settings.setEnabled(options[0], true);
      settings.setEnabled(options[1], true);
    }
  }

  public void testTargetPresentationOmitsModuleLocationAndProvidesIcon() {
    var moduleFile = myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(MyService.class).to(MyServiceImpl.class);
        }
      }
      """);
    var clientFile = myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        @Inject MyService service;
      }
      """);
    myFixture.openFileInEditor(clientFile.getVirtualFile());
    myFixture.doHighlighting();

    Set<GuiceEntry> allEntries = new HashSet<>();
    allEntries.addAll(GuiceEntryProducer.extractFromFile(moduleFile));
    allEntries.addAll(GuiceEntryProducer.extractFromFile(clientFile));
    assertThat(allEntries).isNotEmpty();

    GuiceEntryTargetRenderer renderer = new GuiceEntryTargetRenderer(List.copyOf(allEntries));
    for (GuiceEntry entry : allEntries) {
      PsiElement target = entry.getNavigationTarget();
      assertThat(target).isNotNull();
      TargetPresentation presentation = renderer.getPresentation(target);
      assertThat(presentation.getLocationText()).isNull();
      assertThat(presentation.getContainerText()).isNotNull();
      assertThat(presentation.getIcon()).isNotNull();
    }
  }

  public void testDuplicateSymbolAndFileAppendsLineNumber() {
    var testFile = myFixture.configureByText("IndexedSchemaDataTest.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.Provides;
      public class IndexedSchemaDataTest {
        Object module1() {
          return new AbstractModule() {
            @Provides
            MyService provideRoleMapParser() { return null; }
          };
        }
        Object module2() {
          return new AbstractModule() {
            @Provides
            MyService provideRoleMapParser() { return null; }
          };
        }
      }
      """);
    var otherFile = myFixture.configureByText("OtherModule.java", """
      import com.google.inject.AbstractModule;
      import com.google.inject.Provides;
      public class OtherModule extends AbstractModule {
        @Provides
        MyService provideRoleMapParser() { return null; }
      }
      """);

    Set<GuiceEntry> allEntries = new HashSet<>();
    allEntries.addAll(GuiceEntryProducer.extractFromFile(testFile));
    allEntries.addAll(GuiceEntryProducer.extractFromFile(otherFile));
    assertThat(allEntries).hasSize(3);

    GuiceEntryTargetRenderer renderer = new GuiceEntryTargetRenderer(List.copyOf(allEntries));
    List<String> containerTexts = allEntries.stream()
        .map(GuiceEntry::getNavigationTarget)
        .filter(Objects::nonNull)
        .map(renderer::getContainerText)
        .sorted()
        .toList();

    assertThat(containerTexts).containsExactly(
        "IndexedSchemaDataTest.java:13",
        "IndexedSchemaDataTest.java:7",
        "OtherModule.java"
    );
  }

  public void testSortTargetsByTestStatusAndPackageHierarchy() {
    ExtensionPointName.<TestSourcesFilter>create("com.intellij.testSourcesFilter")
        .getPoint()
        .registerExtension(new TestSourcesFilter() {
          @Override
          public boolean isTestSource(@NotNull VirtualFile file, @NotNull Project project) {
            return file.getName().endsWith("Test.java");
          }
        }, getTestRootDisposable());

    var sourceFile = myFixture.addFileToProject("company/product1/SourceClient.java", """
      package company.product1;
      import com.google.inject.AbstractModule;
      import com.google.inject.Inject;
      import com.google.inject.Provides;
      public class SourceClient {
        @Inject String value;
        static class LocalModule extends AbstractModule {
          @Provides String provideInSameFile() { return ""; }
        }
      }
      """);
    var samePkgFile = myFixture.addFileToProject("company/product1/SamePkgModule.java", """
      package company.product1;
      import com.google.inject.AbstractModule;
      import com.google.inject.Provides;
      public class SamePkgModule extends AbstractModule {
        @Provides String provideSamePkg() { return ""; }
      }
      """);
    var subPkgFile = myFixture.addFileToProject("company/product1/sub/SubPkgModule.java", """
      package company.product1.sub;
      import com.google.inject.AbstractModule;
      import com.google.inject.Provides;
      public class SubPkgModule extends AbstractModule {
        @Provides String provideSubPkg() { return ""; }
      }
      """);
    var deepSubPkgFile = myFixture.addFileToProject("company/product1/sub/impl/DeepSubPkgModule.java", """
      package company.product1.sub.impl;
      import com.google.inject.AbstractModule;
      import com.google.inject.Provides;
      public class DeepSubPkgModule extends AbstractModule {
        @Provides String provideDeepSubPkg() { return ""; }
      }
      """);
    var parentPkgFile = myFixture.addFileToProject("company/ParentPkgModule.java", """
      package company;
      import com.google.inject.AbstractModule;
      import com.google.inject.Provides;
      public class ParentPkgModule extends AbstractModule {
        @Provides String provideParentPkg() { return ""; }
      }
      """);
    var siblingPkgFile = myFixture.addFileToProject("company/product2/SiblingPkgModule.java", """
      package company.product2;
      import com.google.inject.AbstractModule;
      import com.google.inject.Provides;
      public class SiblingPkgModule extends AbstractModule {
        @Provides String provideSiblingPkg() { return ""; }
      }
      """);
    var unrelatedPkgFile = myFixture.addFileToProject("other/domain/UnrelatedModule.java", """
      package other.domain;
      import com.google.inject.AbstractModule;
      import com.google.inject.Provides;
      public class UnrelatedModule extends AbstractModule {
        @Provides String provideUnrelated() { return ""; }
      }
      """);
    var samePkgTestFile = myFixture.addFileToProject("company/product1/SamePkgModuleTest.java", """
      package company.product1;
      import com.google.inject.AbstractModule;
      import com.google.inject.Provides;
      public class SamePkgModuleTest extends AbstractModule {
        @Provides String provideSamePkgTest() { return ""; }
      }
      """);
    var siblingPkgTestFile = myFixture.addFileToProject("company/product2/SiblingPkgModuleTest.java", """
      package company.product2;
      import com.google.inject.AbstractModule;
      import com.google.inject.Provides;
      public class SiblingPkgModuleTest extends AbstractModule {
        @Provides String provideSiblingPkgTest() { return ""; }
      }
      """);

    List<GuiceEntry> entries = new ArrayList<>();
    for (var file : List.of(
        siblingPkgTestFile, unrelatedPkgFile, siblingPkgFile, samePkgTestFile,
        deepSubPkgFile, parentPkgFile, samePkgFile, subPkgFile, sourceFile)) {
      for (GuiceEntry e : GuiceEntryProducer.extractFromFile(file)) {
        if (e.getPresentableText() != null && e.getPresentableText().startsWith("provide")) {
          entries.add(e);
        }
      }
    }

    GuiceEntryTargetRenderer.SortedTargets sorted = GuiceEntryTargetRenderer.sortTargets(entries, sourceFile);
    List<String> orderedNames = sorted.entries().stream()
        .map(GuiceEntry::getPresentableText)
        .toList();

    assertThat(orderedNames).containsExactly(
        "provideInSameFile()",
        "provideSamePkg()",
        "provideSubPkg()",
        "provideDeepSubPkg()",
        "provideParentPkg()",
        "provideSiblingPkg()",
        "provideUnrelated()",
        "provideSamePkgTest()",
        "provideSiblingPkgTest()"
    ).inOrder();
  }
}
