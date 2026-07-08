package com.intellij.lang.puppet.usages;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.lang.puppet.PuppetTestUtil;
import com.intellij.lang.puppet.project.PuppetTestCase;
import com.intellij.lang.puppet.psi.PuppetLazyProxyLightElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.testFramework.TestFrameworkUtil;
import com.intellij.usageView.UsageInfo;
import junit.framework.TestSuite;

import java.util.Collection;

public class PuppetFindUsagesTest extends PuppetTestCase {
  @Override
  protected String getBasePath() {
    return "usages";
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    setUpLibraries();
  }

  public void testVariables() {
    doTest(6);
  }

  public void testQualifiedLibClass() {
    doTest(3);
  }

  public void testQualifiedLibResource() {
    doTest(2);
  }

  public void testQualifiedScopedClass() {
    doTest(1);
  }

  public void testQualifiedLibVariable() {
    doTest(2);
  }

  public void testLazyProxyElement() {
    doTest(2);
    PsiReference reference = myFixture.getReferenceAtCaretPosition();
    assertNotNull(reference);
    assertInstanceOf(reference.resolve(), PuppetLazyProxyLightElement.class);
  }

  private void doTest(int expectedCount) {
    String testName = getTestName(true);
    String testFileName = testName + ".code";

    configureByManifest(testFileName);
    int flags = TargetElementUtil.ELEMENT_NAME_ACCEPTED | TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED;
    PsiElement targetElement = TargetElementUtil.findTargetElement(myFixture.getEditor(), flags);
    assertNotNull("Cannot find referenced element", targetElement);
    Collection<UsageInfo> usages = myFixture.findUsages(targetElement);
    assertEquals(expectedCount, usages.size());
  }

  public static TestSuite suite() {
    return TestFrameworkUtil.flattenSuite(PuppetTestUtil.createTestSuiteForVersions(PuppetFindUsagesTest.class));
  }
}
