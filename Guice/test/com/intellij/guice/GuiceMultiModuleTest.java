// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice;

import com.intellij.codeInsight.daemon.GutterMark;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.guice.model.GuiceProjectModel;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

public class GuiceMultiModuleTest extends JavaCodeInsightFixtureTestCase {

  public void testMultiModuleDiscoveryWhenDependencyModuleHighlightedFirst() throws Exception {
    Sdk sdk = JavaSdk.getInstance().createJdk("TestJDK", System.getProperty("java.home"), false);
    WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      ProjectJdkTable.getInstance().addJdk(sdk, getTestRootDisposable());
    });
    Module baseModule = myFixture.getModule();
    configureGuiceModule(baseModule, sdk);

    VirtualFile appDir = myFixture.getTempDirFixture().findOrCreateDir("app");
    Module appModule = PsiTestUtil.addModule(getProject(), JavaModuleType.getModuleType(), "app", appDir);
    configureGuiceModule(appModule, sdk);
    ModuleRootModificationUtil.addDependency(appModule, baseModule);

    myFixture.addClass("package shared; public interface MyService {}");
    myFixture.addClass("package shared; public class MyServiceImpl implements MyService { public MyServiceImpl() {} }");

    WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      try {
        VirtualFile vf = appDir.createChildData(this, "AppModule.java");
        VfsUtil.saveText(vf, """
          import com.google.inject.AbstractModule;
          import shared.MyService;
          import shared.MyServiceImpl;
          public class AppModule extends AbstractModule {
            @Override
            protected void configure() {
              bind(MyService.class).to(MyServiceImpl.class);
            }
          }
          """);
      }
      catch (java.io.IOException e) {
        throw new RuntimeException(e);
      }
    });
    PsiDocumentManager.getInstance(getProject()).commitAllDocuments();

    myFixture.configureByText("Client.java", """
      package shared;
      import com.google.inject.Inject;
      public class Client {
        @Inject
        MyService service;
      }
      """);

    // Reset the model so VFS events from test setup do not mask initial discovery when highlighting Client.java in baseModule first.
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    GuiceProjectModel.getInstance(getProject()).dispose();

    myFixture.doHighlighting();
    List<String> anchors = new ArrayList<>();
    for (GutterMark mark : myFixture.findAllGutters()) {
      if (GuiceTestBase.TO_BINDINGS_TOOLTIP.equals(mark.getTooltipText())) {
        var info = ((LineMarkerInfo.LineMarkerGutterIconRenderer<?>)mark).getLineMarkerInfo();
        PsiElement element = info.getElement();
        anchors.add(element == null ? "<invalid>" : element.getText());
      }
    }
    assertThat(anchors).containsExactly("service");
  }

  private static void configureGuiceModule(Module module, Sdk sdk) {
    ModuleRootModificationUtil.setModuleSdk(module, sdk);
    ModuleRootModificationUtil.updateModel(module, model -> {
      Path jar = Path.of(PathManager.getJarPathForClass(com.google.inject.Inject.class));
      PsiTestUtil.addLibrary(model, jar.getFileName().toString(), jar.getParent().toString(), jar.getFileName().toString());
    });
  }
}
