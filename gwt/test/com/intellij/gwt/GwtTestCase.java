/*
 * Copyright (c) 2000-2006 JetBrains s.r.o. All Rights Reserved.
 */
package com.intellij.gwt;

import com.intellij.facet.FacetManager;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.facet.impl.FacetUtil;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.facet.GwtFacetConfiguration;
import com.intellij.gwt.facet.GwtFacetType;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.gwt.sdk.impl.GwtSdkImpl;
import com.intellij.gwt.sdk.impl.GwtVersionImpl;
import com.intellij.javaee.DeploymentDescriptorsConstants;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.javaee.web.facet.WebFacetType;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.IndexingTestUtil;
import com.intellij.testFramework.JavaPsiTestCase;
import com.intellij.testFramework.PsiTestUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public abstract class GwtTestCase extends JavaPsiTestCase {
  @NonNls private static final String MOCK_GWT_USER_JAR_NAME = "gwt-user.jar";

  @NotNull
  protected <T extends PsiElement> T findElementByString(final VirtualFile file, final String s, final Class<T> aClass) {
    final PsiFile psiFile = getPsiManager().findFile(file);
    assertNotNull(psiFile);
    final int offset = psiFile.getText().indexOf(s);
    assertTrue(offset >= 0);
    final PsiElement element = psiFile.findElementAt(offset);
    assertNotNull(element);
    final T parent = PsiTreeUtil.getParentOfType(element, aClass, false);
    assertNotNull(parent);
    return parent;
  }

  protected VirtualFile addGwtModule(@NonNls final String testDataPath, String... sourceDirs) {
    return addGwtModule(testDataPath, myModule, sourceDirs);
  }

  protected VirtualFile addGwtModule(@NonNls final String testDataPath, final Module module, String... sourceDirs) {
    return addGwtModule(testDataPath, module, GwtVersionImpl.VERSION_1_4, sourceDirs);
  }

  protected VirtualFile addGwtModule(@NonNls final String testDataPath,
                                     final Module module,
                                     final GwtVersion version,
                                     final String... sourceDirs) {
    addGwtLibrary(module, version);

    VirtualFile moduleRoot = getTempDir().createVirtualDir();
    try {
      WriteCommandAction.writeCommandAction(getProject()).run(() -> {
        final String relativePath = FileUtil.toSystemDependentName(testDataPath);
        final VirtualFile testData = LocalFileSystem.getInstance().refreshAndFindFileByPath(getGwtTestDataPath() + relativePath);
        assertNotNull(relativePath, testData);
        VfsUtil.copyDirectory(this, testData, moduleRoot, VirtualFileFilter.ALL);

        if (sourceDirs.length == 0) {
          PsiTestUtil.addSourceRoot(module, moduleRoot);
        }
        else {
          PsiTestUtil.addContentRoot(module, moduleRoot);
          for (String sourceDir : sourceDirs) {
            PsiTestUtil.addSourceRoot(module, moduleRoot.findFileByRelativePath(sourceDir));
          }
        }

        addGwtFacet(module, version);
      });
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }


    return moduleRoot;
  }

  protected GwtVersionImpl getLatestVersion() {
    return GwtVersionImpl.VERSION_2_0;
  }

  public static GwtFacet addGwtFacet(final Module module, @Nullable final GwtVersion gwtVersion) {
    return WriteCommandAction.runWriteCommandAction(null, (Computable<GwtFacet>)() -> {
      String url = VfsUtilCore.pathToUrl(getMockGwtSdkPath(gwtVersion));
      GwtFacet gwtFacet = GwtFacet.getInstance(module);
      if (gwtFacet == null) {
        GwtFacetConfiguration configuration = new GwtFacetConfiguration();
        final ModifiableFacetModel facetModel = FacetManager.getInstance(module).createModifiableModel();
        gwtFacet = new GwtFacet(GwtFacetType.getInstance(), module, GwtFacetType.getInstance().getPresentableName(), configuration);
        facetModel.addFacet(gwtFacet);
        facetModel.commit();
      }

      gwtFacet.getConfiguration().setGwtSdkUrl(url);
      ((GwtSdkImpl)gwtFacet.getConfiguration().getSdk()).setVersion(gwtVersion != null ? gwtVersion : GwtVersionImpl.VERSION_1_4);
      IndexingTestUtil.waitUntilIndexesAreReady(module.getProject());
      return gwtFacet;
    });
  }

  public static void addGwtLibrary(final Module module, GwtVersion version) {
    String path = getMockGwtUserJarPath(version);
    final VirtualFile jarFile = JarFileSystem.getInstance().findFileByPath(path);
    assertNotNull(jarFile);
    ModuleRootModificationUtil.addModuleLibrary(module, jarFile.getUrl());
    IndexingTestUtil.waitUntilIndexesAreReady(module.getProject());
  }

  public static String getMockGwtUserJarPath(@Nullable GwtVersion version) {
    return getMockGwtSdkPath(version) + "/" + MOCK_GWT_USER_JAR_NAME + JarFileSystem.JAR_SEPARATOR;
  }

  private static String getMockGwtSdkPath(final @Nullable GwtVersion gwtVersion) {
    GwtVersionImpl version = (GwtVersionImpl)gwtVersion;
    String versionString = version == null || version.isAtLeast(GwtVersionImpl.VERSION_2_7) ? "2.7" :
                           version.isAtLeast(GwtVersionImpl.VERSION_2_6) ? "2.6" :
                           version.isAtLeast(GwtVersionImpl.VERSION_2_0) ? "2.1" :
                           version.isAtLeast(GwtVersionImpl.VERSION_1_6) ? "1.6" :
                           version.isAtLeast(GwtVersionImpl.VERSION_1_5) ? "1.5" : "1.3";
    return FileUtil.toSystemIndependentName(getGwtTestDataPath()) + "sdk/" + versionString;
  }

  public static String getGwtTestDataPath() {
    // In the standalone Gradle build the test data lives in the project's testData/ directory,
    // provided via the 'gwt.test.data.path' system property (see build.gradle.kts). Fall back to
    // the historical monorepo layout when the property is absent.
    String override = System.getProperty("gwt.test.data.path");
    if (override != null && !override.isEmpty()) {
      if (!override.endsWith("/")) override += "/";
      return FileUtil.toSystemDependentName(override);
    }
    return PathManager.getHomePath() + FileUtil.toSystemDependentName("/plugins/GwtStudio/testData/");
  }

  protected GwtModulesManager getGwtManager() {
    return GwtModulesManager.getInstance(myProject);
  }

  public static WebFacet addWebFacet(final GwtFacet gwtFacet, final String testDataPath, final String webXmlPath) {
    return WriteAction.computeAndWait(() -> {
      WebFacet webFacet = FacetUtil.addFacet(gwtFacet.getModule(), WebFacetType.getInstance());
      final String webXmlUrl = VfsUtilCore.pathToUrl(testDataPath) + "/" + webXmlPath;
      webFacet.getDescriptorsContainer().getConfiguration().addConfigFile(DeploymentDescriptorsConstants.WEB_XML_META_DATA, webXmlUrl);
      gwtFacet.getConfiguration().setWebFacetName(webFacet.getName());
      return webFacet;
    });
  }

  protected GwtModule findGwtModule(final String qualifiedName) {
    GlobalSearchScope allScope = GlobalSearchScope.allScope(myProject);
    final GwtModule module = getGwtManager().findGwtModuleByQualifiedName(qualifiedName, allScope);
    assertNotNull(module);
    return module;
  }
}
