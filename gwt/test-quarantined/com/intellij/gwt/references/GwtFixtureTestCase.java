package com.intellij.gwt.references;

import com.intellij.ee.core.javaee.JavaeeTestUtil;
import com.intellij.gwt.GwtTestCase;
import com.intellij.gwt.GwtTestOptions;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.psi.GwtSourcePathsRefresher;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.gwt.sdk.impl.GwtVersionImpl;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.IndexingTestUtil;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.builders.WebModuleFixtureBuilder;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture;
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import com.intellij.util.CommonProcessors;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.Method;

public abstract class GwtFixtureTestCase extends UsefulTestCase {
  protected WebModuleFixtureBuilder myModuleBuilder;
  protected IdeaProjectTestFixture myProjectFixture;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    final TestFixtureBuilder<IdeaProjectTestFixture> fixtureBuilder = JavaTestFixtureFactory.createFixtureBuilder(getName());
    myModuleBuilder = fixtureBuilder.addModule(WebModuleFixtureBuilder.class);
    myProjectFixture = fixtureBuilder.getFixture();
  }

  protected void addGwtSupport() {
    GwtVersion version = getGwtVersion();
    Module module = myProjectFixture.getModule();
    GwtFacet facet = GwtTestCase.addGwtFacet(module, version);
    WebFacet webFacet = JavaeeTestUtil.getWebFacet(module);
    if (webFacet != null) {
      facet.getConfiguration().setWebFacetName(webFacet.getName());
    }
    GwtTestCase.addGwtLibrary(module, version);
  }

  @NotNull
  private GwtVersion getGwtVersion() {
    try {
      Method method = getClass().getMethod(getName());
      GwtTestOptions options = method.getAnnotation(GwtTestOptions.class);
      if (options == null) {
        options = getClass().getAnnotation(GwtTestOptions.class);
      }
      if (options != null) {
        return options.version();
      }
      return GwtVersionImpl.VERSION_2_0;
    }
    catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  protected JavaCodeInsightTestFixture createCodeInsightFixture(final String relativeTestDataPath) throws Exception {
    final String testDataPath = GwtTestCase.getGwtTestDataPath() + relativeTestDataPath;
    final JavaCodeInsightTestFixture codeInsightFixture = JavaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(myProjectFixture);
    codeInsightFixture.setTestDataPath(testDataPath);
    final TempDirTestFixture tempDir = codeInsightFixture.getTempDirFixture();
    final String contentRoot = tempDir.getTempDirPath();
    myModuleBuilder.addContentRoot(contentRoot);
    if (new File(testDataPath, "src").exists()) {
      final String sourceRoot = contentRoot + "/src";
      new File(FileUtil.toSystemDependentName(sourceRoot)).mkdirs();
      myModuleBuilder.addSourceRoot(sourceRoot);

      if (new File(testDataPath, "resources").exists()) {
        String resourcesRoot = contentRoot + "/resources";
        new File(FileUtil.toSystemDependentName(resourcesRoot)).mkdirs();
        myModuleBuilder.addSourceRoot(resourcesRoot);
      }
    }
    else {
      myModuleBuilder.addSourceRoot(contentRoot);
    }

    if (new File(testDataPath, "web").exists()) {
      final String webRoot = contentRoot + "/web";
      new File(FileUtil.toSystemDependentName(webRoot)).mkdirs();
      myModuleBuilder.addWebRoot(webRoot, "/");
    }
    setupModule(myModuleBuilder);

    codeInsightFixture.setUp();

    final VirtualFile dir = LocalFileSystem.getInstance().refreshAndFindFileByPath(testDataPath);
    VfsUtilCore.processFilesRecursively(dir, new CommonProcessors.CollectProcessor<>());
    dir.refresh(false, true);
    tempDir.copyAll(testDataPath, "", file -> !file.getName().contains("_after"));


    waitForGwtSourcePathsRefresher(myProjectFixture.getProject());

    return codeInsightFixture;
  }

   public static void waitForGwtSourcePathsRefresher(Project project) {
    IndexingTestUtil.waitUntilIndexesAreReady(project);
    // Give VFS events time to propagate and reach the update queue
    try {
      Thread.sleep(10);
    }
    catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    PlatformTestUtil.waitWithEventsDispatching("GwtSourcePathsRefresher", () -> GwtSourcePathsRefresher.getInstance(project).isAllExecuted(), 60);
    IndexingTestUtil.waitUntilIndexesAreReady(project);
  }

  protected void setupModule(WebModuleFixtureBuilder moduleBuilder) {
  }
}
