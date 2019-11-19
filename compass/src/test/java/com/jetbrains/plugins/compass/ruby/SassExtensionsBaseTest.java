package com.jetbrains.plugins.compass.ruby;

import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.ui.UIUtil;
import com.jetbrains.plugins.compass.CompassSettings;
import com.jetbrains.plugins.compass.CompassUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.ruby.gem.GemDependency;
import org.jetbrains.plugins.ruby.gem.GemInfo;
import org.jetbrains.plugins.ruby.gem.GemManager;
import org.jetbrains.plugins.ruby.rails.testCases.RailsGemSuiteTestCase;
import org.jetbrains.plugins.sass.extensions.SassRubyIntegrationHelper;

import java.util.Collections;

abstract public class SassExtensionsBaseTest extends RailsGemSuiteTestCase {
  protected String COMPASS_STYLESHEET_PATH = null;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    //noinspection ConstantConditions
    COMPASS_STYLESHEET_PATH = GemManager.findGem(myFixture.getModule(), CompassUtil.COMPASS_GEM_NAME).getFile().getPath()
                              + "/frameworks/compass/stylesheets";
    CompassSettings compassSettings = CompassSettings.getInstance(myFixture.getModule());
    compassSettings.setCompassSupportEnabled(true);
    CompassUtil.removeCompassLibraryIfNeeded(myFixture.getModule());
    SassRubyIntegrationHelper.getInstance().getCompassExtension().startActivity(myFixture.getModule());
    UIUtil.dispatchAllInvocationEvents();
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      SassRubyIntegrationHelper.getInstance().getCompassExtension().stopActivity(myFixture.getModule());
      CompassSettings.getInstance(myFixture.getModule()).setImportPaths(Collections.emptyList());
      UIUtil.dispatchAllInvocationEvents();
      COMPASS_STYLESHEET_PATH = null;
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      super.tearDown();
    }
  }

  @NotNull
  @Override
  protected RailsLightProjectDescriptor createProjectDescriptor() {
    final RailsLightProjectDescriptor descriptor = super.createProjectDescriptor();
    attachGemsToProjectDescriptor(descriptor);

    return descriptor;
  }

  protected void attachGemsToProjectDescriptor(@NotNull final RailsLightProjectDescriptor descriptor) {
    descriptor.addGemToAttach("compass");

    final GemDependency railsDependency = descriptor.getRailsDependency();
    if (railsDependency.isUnsatisfied("4.0.0") && railsDependency.isUnsatisfied("4.1.0")) {
      descriptor.addGemToAttach("sass");
      descriptor.addGemToAttach("sprockets");
    }
  }

  protected String getTestFileName() {
    return getTestFileName("scss");
  }

  private String getTestFileName(@NotNull String extension) {
    return getTestName(true) + "." + extension;
  }

  protected void assertAncestorOfGem(@NotNull PsiElement psiElement, @NotNull String gemName) {
    final GemInfo gem = GemManager.findGem(myFixture.getModule(), gemName);
    assertNotNull(gem);
    final VirtualFile gemFile = gem.getFile();
    assertNotNull(gemFile);
    final VirtualFile resolvedFile = psiElement.getContainingFile().getVirtualFile();
    assertTrue("gem path: " + gemFile.getPath() + "; resolved file: " + resolvedFile.getPath(),
               VfsUtilCore.isAncestor(gemFile, resolvedFile, false));
  }

  @NotNull
  protected PsiElement resolve() {
    return resolve("scss");
  }

  @NotNull
  protected PsiElement resolve(@NotNull String extension) {
    myFixture.configureByFile(getTestFileName(extension));
    final PsiReference at = myFixture.getFile().findReferenceAt(myFixture.getCaretOffset());
    assertNotNull(at);
    final PsiElement element = at.resolve();
    assertNotNull(element);
    return element;
  }
}
