package com.intellij.guice;

import static com.intellij.codeInsight.daemon.LineMarkerInfo.LineMarkerGutterIconRenderer;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.intellij.codeInsight.daemon.GutterMark;
import com.intellij.codeInsight.navigation.NavigationGutterIconRenderer;
import com.intellij.guice.model.GuiceNavigationIndex;
import com.intellij.guice.model.GuiceProjectModel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public abstract class GuiceTestBase extends LightJavaCodeInsightFixtureTestCase {
  private static final LightProjectDescriptor DESCRIPTOR =
    new DefaultLightProjectDescriptor() {
      @Override
      public Sdk getSdk() {
        String javaHome = System.getProperty("java.home");
        return JavaSdk.getInstance().createJdk("Current JDK", javaHome, false);
      }

      @Override
      public void configureModule(@NotNull Module module,
                                  @NotNull ModifiableRootModel model,
                                  @NotNull ContentEntry contentEntry) {
        super.configureModule(module, model, contentEntry);
        // Kotlin test sources need the standard library (TODO(), ::class.java, type inference for bind()).
        // The tested IDE distribution ships it with the Kotlin plugin, so no network access is necessary.
        // Do not use the jar that holds kotlin.Unit at runtime: it is a platform jar that K2 does not treat as the stdlib.
        Path stdlibJar = Path.of(PathManager.getHomePath(),
                                 "plugins", "Kotlin", "kotlinc", "lib", "kotlin-stdlib.jar");
        assert Files.isRegularFile(stdlibJar) : "No Kotlin stdlib at " + stdlibJar;
        PsiTestUtil.addLibrary(model, "kotlin-stdlib", stdlibJar.getParent().toString(),
                                                          stdlibJar.getFileName().toString());
        // The real Guice API. Gradle puts these jars on the test classpath (see build.gradle.kts).
        for (Class<?> apiClass : List.of(com.google.inject.Inject.class,
                                         FactoryModuleBuilder.class,
                                         javax.inject.Inject.class,
                                         jakarta.inject.Inject.class)) {
          Path jar = Path.of(PathManager.getJarPathForClass(apiClass));
          PsiTestUtil.addLibrary(model, jar.getFileName().toString(), jar.getParent().toString(),
                                 jar.getFileName().toString());
        }
      }
    };

  @Override
  protected LightProjectDescriptor getProjectDescriptor() {
    return DESCRIPTOR;
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    // Add dummy classes for binding targets
    myFixture.addClass("public interface MyKey {}");
    myFixture.addClass("public interface MyService {}");
    myFixture.addClass("public class MyServiceImpl implements MyService { public MyServiceImpl() {} }");
    myFixture.addClass("""
      public class MyServiceProvider implements javax.inject.Provider<MyService> {
        public MyService get() { return null; }
      }
      """);
  }

  protected static final String TO_BINDINGS_TOOLTIP = "Navigate to Guice bind expression";
  protected static final String TO_INJECTION_POINTS_TOOLTIP = "Navigate to injection points";

  /**
   * Highlights the file in the editor and returns the navigation targets of all gutter icons with the tooltip.
   * Each target is given as the text of the target element with collapsed whitespace, sorted.
   */
  protected java.util.List<String> gutterTargets(@NotNull String tooltip) {
    myFixture.doHighlighting();
    java.util.List<String> result = new ArrayList<>();
    for (GutterMark mark : myFixture.findAllGutters()) {
      if (!tooltip.equals(mark.getTooltipText())) continue;
      assertInstanceOf(mark, LineMarkerGutterIconRenderer.class);
      var info = ((LineMarkerGutterIconRenderer<?>)mark).getLineMarkerInfo();
      var handler = info.getNavigationHandler();
      assertInstanceOf(handler, NavigationGutterIconRenderer.class);
      for (PsiElement target : ((NavigationGutterIconRenderer)handler).getTargetElements()) {
        result.add(target.getText().replaceAll("\\s+", " "));
      }
    }
    java.util.Collections.sort(result);
    return result;
  }

  /**
   * Returns the text of the elements that carry a gutter icon with the tooltip, sorted.
   */
  protected java.util.List<String> gutterAnchors(@NotNull String tooltip) {
    myFixture.doHighlighting();
    java.util.List<String> result = new ArrayList<>();
    for (GutterMark mark : myFixture.findAllGutters()) {
      if (!tooltip.equals(mark.getTooltipText())) continue;
      var info = ((LineMarkerGutterIconRenderer<?>)mark).getLineMarkerInfo();
      PsiElement element = info.getElement();
      result.add(element == null ? "<invalid>" : element.getText());
    }
    java.util.Collections.sort(result);
    return result;
  }

  protected GuiceNavigationIndex getNavigationIndex() {
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    final GuiceNavigationIndex[] indexHolder = new GuiceNavigationIndex[1];
    ProgressManager.getInstance().runProcess(() -> {
      ApplicationManager.getApplication().runReadAction(() -> {
        indexHolder[0] = GuiceProjectModel.getInstance(getProject()).getNavigationIndex(myFixture.getModule());
      });
    }, new EmptyProgressIndicator());
    return indexHolder[0];
  }
}
