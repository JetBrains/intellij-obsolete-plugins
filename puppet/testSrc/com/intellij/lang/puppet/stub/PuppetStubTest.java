package com.intellij.lang.puppet.stub;

import com.intellij.lang.puppet.OnVersion;
import com.intellij.lang.puppet.PuppetFileType;
import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.PuppetResolveTestUtil;
import com.intellij.lang.puppet.PuppetTestUtil;
import com.intellij.lang.puppet.project.PuppetTestCase;
import com.intellij.lang.puppet.psi.PuppetPsiFileImpl;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.stubs.ObjectStubTree;
import com.intellij.psi.stubs.StubTreeLoader;
import com.intellij.testFramework.TestFrameworkUtil;
import com.intellij.testFramework.UsefulTestCase;
import junit.framework.TestSuite;

import java.util.List;

public class PuppetStubTest extends PuppetTestCase {
  @Override
  protected String getBasePath() {
    return "stub";
  }

  @OnVersion(PuppetLanguage.Version.PUPPET_3)
  public void testResolveAllPuppet3() {
    doTest();
  }

  @OnVersion(PuppetLanguage.Version.PUPPET_4)
  public void testResolveAllPuppet4() {
    doTest();
  }

  public void doTest() {
    final String sourceFileName = getTestName(true) + "." + PuppetFileType.DEFAULT_EXTENSION;
    String stubTreeFilename = getTestDataPath() + "/" + getTestName(true) + ".txt";

    myFixture.copyFileToProject("sideFile.pp");
    PuppetPsiFileImpl puppetFile = prepareFile(sourceFileName);

    PsiManagerEx.getInstanceEx(getProject()).setAssertOnFileLoadingFilter(file -> {
      if (file.getName().equals(sourceFileName)) {
        return false;
      }
      if (!"pp".equals(file.getExtension())) {
        return false;
      }
      return true;
    }, myFixture.getTestRootDisposable());

    String stubTreeString = DebugUtil.stubTreeToString(puppetFile.getStub());
    UsefulTestCase.assertSameLinesWithFile(stubTreeFilename, stubTreeString);

    List<PsiPolyVariantReference> references = PuppetResolveTestUtil.getReferencesInFile(puppetFile, PsiPolyVariantReference.class);
    for (PsiPolyVariantReference reference : references) {
      reference.multiResolve(false);
    }
  }

  private PuppetPsiFileImpl prepareFile(String path) {
    VirtualFile virtualFile = myFixture.copyFileToProject(path);
    assertNotNull(virtualFile);

    PuppetPsiFileImpl file = (PuppetPsiFileImpl)((PsiManagerEx)getPsiManager()).getFileManager().findFile(virtualFile);

    assertNotNull(file);
    assertFalse(file.getNode().isParsed());

    ObjectStubTree tree = StubTreeLoader.getInstance().readOrBuild(getProject(), virtualFile, file);
    assertNotNull(tree);

    ((PsiManagerEx)getPsiManager()).cleanupForNextTest();

    return (PuppetPsiFileImpl)getPsiManager().findFile(virtualFile);
  }

  public static TestSuite suite() {
    return TestFrameworkUtil.flattenSuite(PuppetTestUtil.createTestSuiteForVersions(PuppetStubTest.class));
  }
}
