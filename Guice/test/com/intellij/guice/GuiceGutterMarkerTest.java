package com.intellij.guice;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.guice.model.GuiceEntry;
import com.intellij.guice.model.renderers.GuiceEntryTargetRenderer;
import com.intellij.ide.plugins.DynamicPlugins;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginMainDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 * Checks the gutter markers as line-marker objects: the {@code Navigate | Related Symbol} items,
 * the Kotlin class gutter, the popup order, and the dynamic-unload contract of the plugin.
 */
public class GuiceGutterMarkerTest extends GuiceTestBase {

  private static List<String> relatedItemTexts(RelatedItemLineMarkerInfo<?> marker) {
    List<String> result = new ArrayList<>();
    for (GotoRelatedItem item : marker.createGotoRelatedItems()) {
      assertThat(item.getGroup()).isEqualTo("Guice");
      PsiElement element = item.getElement();
      result.add(element == null ? "<null>" : element.getText().replaceAll("\\s+", " "));
    }
    return result;
  }

  public void testInjectionPointGutterExposesGuiceRelatedItems() {
    myFixture.addFileToProject("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
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

    var markers = guiceMarkers(TO_BINDINGS_TOOLTIP);
    assertThat(markers).hasSize(1);
    assertThat(relatedItemTexts(markers.getFirst())).containsExactly("bind(MyService.class).to(MyServiceImpl.class)");
  }

  public void testClassGutterExposesGuiceRelatedItems() {
    myFixture.addClass("""
      import com.google.inject.Inject;
      public class Client {
        @Inject
        Api api;
      }
      """);
    myFixture.addClass("public class ApiImpl implements Api { @com.google.inject.Inject public ApiImpl() {} }");
    myFixture.configureByText("Api.java", """
      @com.google.inject.ImplementedBy(ApiImpl.class)
      public interface Api {}
      """);

    var toInjectionPoints = guiceMarkers(TO_INJECTION_POINTS_TOOLTIP);
    assertThat(toInjectionPoints).hasSize(1);
    assertThat(relatedItemTexts(toInjectionPoints.getFirst())).containsExactly("@Inject Api api;");
  }

  public void testKotlinClassGutterShowsInjectionPointsAndImplementation() {
    myFixture.addClass("""
      import com.google.inject.Inject;
      public class Client {
        @Inject
        Api api;
      }
      """);
    myFixture.addClass("public class ApiImpl implements Api { @com.google.inject.Inject public ApiImpl() {} }");
    myFixture.configureByText("Api.kt", """
      @com.google.inject.ImplementedBy(ApiImpl::class)
      interface Api
      """);

    assertThat(gutterAnchors(TO_INJECTION_POINTS_TOOLTIP)).containsExactly("Api");
    assertThat(gutterTargets(TO_INJECTION_POINTS_TOOLTIP)).containsExactly("@Inject Api api;");
    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("Api");
    assertThat(gutterTargets(TO_BINDINGS_TOOLTIP)).containsExactly("@com.google.inject.Inject public ApiImpl() {}");
  }

  public void testKotlinImplementationClassGutterNavigatesToBinding() {
    myFixture.addFileToProject("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(MyService.class).to(KtImpl.class);
        }
      }
      """);
    myFixture.configureByText("KtImpl.kt", """
      class KtImpl : MyService
      """);

    assertThat(gutterAnchors(TO_BINDINGS_TOOLTIP)).containsExactly("KtImpl");
    assertThat(gutterTargets(TO_BINDINGS_TOOLTIP)).containsExactly("bind(MyService.class).to(KtImpl.class)");
  }

  public void testPopupOrderIsTotalForSameFileNameInDifferentPackages() {
    // The default-package MyService is not visible from a named package, so the test brings its own type.
    myFixture.addFileToProject("svc/Svc.java", "package svc;\npublic interface Svc {}\n");
    myFixture.addFileToProject("svc/SvcImpl.java", "package svc;\npublic class SvcImpl implements Svc {}\n");
    PsiFile first = myFixture.addFileToProject("a/Client.java", """
      package a;
      import com.google.inject.Inject;
      public class Client {
        @Inject
        svc.Svc service;
      }
      """);
    PsiFile second = myFixture.addFileToProject("b/Client.java", """
      package b;
      import com.google.inject.Inject;
      public class Client {
        @Inject
        svc.Svc service;
      }
      """);
    myFixture.configureByText("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(svc.Svc.class).to(svc.SvcImpl.class);
        }
      }
      """);
    var markers = guiceMarkers(TO_INJECTION_POINTS_TOOLTIP);
    assertThat(markers).hasSize(1);
    PsiElement anchor = markers.getFirst().getElement();
    assertNotNull(anchor);

    var index = getNavigationIndex();
    List<GuiceEntry> counterparts = new ArrayList<>();
    for (GuiceEntry entry : index.findEntriesByAnchor(anchor)) {
      counterparts.addAll(index.findCounterparts(entry));
    }
    assertThat(counterparts).hasSize(2);

    // Same test/production status, same package distance, same text, same file name and same offset:
    // the order must still not depend on the input order.
    var forward = GuiceEntryTargetRenderer.sortTargets(counterparts, anchor).targets();
    var backward = GuiceEntryTargetRenderer.sortTargets(List.of(counterparts.get(1), counterparts.get(0)), anchor).targets();
    assertThat(forward).isEqualTo(backward);
    assertThat(forward.get(0).getContainingFile()).isEqualTo(first);
    assertThat(forward.get(1).getContainingFile()).isEqualTo(second);

    // Both targets get a line number, because the file name alone does not tell them apart.
    var renderer = new GuiceEntryTargetRenderer(List.copyOf(counterparts));
    assertThat(renderer.getContainerText(forward.get(0))).isEqualTo("Client.java:5");
    assertThat(renderer.getContainerText(forward.get(1))).isEqualTo("Client.java:5");
  }

  /**
   * On a dynamic unload the platform removes the plugin's highlighters from the open editors, and it recognises
   * a line marker by the class of its {@code LineMarkerInfo}. A platform {@code RelatedItemLineMarkerInfo} would
   * stay in the gutter and keep the plugin class loader alive through the target-renderer lambda.
   */
  public void testGuiceLineMarkersUseAPluginOwnedMarkerClass() {
    PsiFile apiFile = myFixture.addClass("public interface Api {}").getContainingFile();
    myFixture.addClass("public class ApiImpl implements Api {}");
    myFixture.addFileToProject("MyModule.java", """
      import com.google.inject.AbstractModule;
      public class MyModule extends AbstractModule {
        @Override
        protected void configure() {
          bind(Api.class).to(ApiImpl.class);
          bind(MyService.class).to(MyServiceImpl.class);
        }
      }
      """);
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        @Inject
        Api api;
        @Inject
        MyService service;
      }
      """);
    var injectionMarkers = guiceMarkers(TO_BINDINGS_TOOLTIP);
    assertThat(injectionMarkers).hasSize(2);
    for (RelatedItemLineMarkerInfo<?> marker : injectionMarkers) {
      assertThat(marker.getClass().getName()).startsWith("com.intellij.guice.");
    }

    // The class gutter of GuiceBindingClassAnnotator: Api is the key of bind(Api.class).
    myFixture.openFileInEditor(apiFile.getVirtualFile());
    var classMarkers = guiceMarkers(TO_BINDINGS_TOOLTIP);
    assertThat(classMarkers).hasSize(1);
    for (RelatedItemLineMarkerInfo<?> marker : classMarkers) {
      assertThat(marker.getClass().getName()).startsWith("com.intellij.guice.");
    }
  }

  public void testPluginCanBeUnloadedWithoutRestart() {
    IdeaPluginDescriptor descriptor = PluginManagerCore.getPlugin(PluginId.getId("com.intellij.guice"));
    assertNotNull("The Guice plugin must be loaded in the test IDE", descriptor);
    assertThat(descriptor).isInstanceOf(PluginMainDescriptor.class);
    String reason = DynamicPlugins.INSTANCE.checkCanUnloadWithoutRestart((PluginMainDescriptor)descriptor);
    assertThat(reason).isNull();
  }
}
