package com.intellij.guice.model;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase;

import java.nio.file.Path;

import static com.google.common.truth.Truth.assertThat;

/**
 * Checks when the {@link GuiceProjectModel} service comes to life, and that it follows the project structure
 * without a start-up activity.
 */
public class GuiceServiceLifecycleTest extends JavaCodeInsightFixtureTestCase {

  public void testServiceIsCreatedOnlyForGuiceModulesAndFollowsLibraryChanges() {
    Sdk sdk = JavaSdk.getInstance().createJdk("TestJDK", System.getProperty("java.home"), false);
    WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      ProjectJdkTable.getInstance().addJdk(sdk, getTestRootDisposable());
    });
    Module module = myFixture.getModule();
    ModuleRootModificationUtil.setModuleSdk(module, sdk);

    // A6: a project without Guice never creates the service, even after highlighting a Java file.
    myFixture.configureByText("Plain.java", "public class Plain { void m() {} }");
    myFixture.doHighlighting();
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    assertNull(getProject().getServiceIfCreated(GuiceProjectModel.class));

    // Guice arrives: the first highlighting of a Guice-enabled module creates the service and builds the index.
    addLibraryOf(module, com.google.inject.Inject.class);
    myFixture.addClass("public interface Svc {}");
    myFixture.configureByText("Client.java", """
      import com.google.inject.Inject;
      public class Client {
        @Inject
        Svc svc;
      }
      """);
    myFixture.doHighlighting();
    GuiceProjectModel model = getProject().getServiceIfCreated(GuiceProjectModel.class);
    assertNotNull(model);
    assertThat(model.isIndexActive()).isTrue();
    // The replayed workspace event at subscription time must not count as a structure change.
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    assertThat(model.isPopulationNeeded()).isFalse();

    // A real structure change after the service exists is seen without a start-up activity.
    addLibraryOf(module, jakarta.inject.Inject.class);
    PlatformTestUtil.waitWithEventsDispatching("The workspace model change did not reach the Guice model",
                                               model::isPopulationNeeded, 10);
  }

  private static void addLibraryOf(Module module, Class<?> libraryClass) {
    ModuleRootModificationUtil.updateModel(module, model -> {
      Path jar = Path.of(PathManager.getJarPathForClass(libraryClass));
      PsiTestUtil.addLibrary(model, jar.getFileName().toString(), jar.getParent().toString(), jar.getFileName().toString());
    });
  }
}
