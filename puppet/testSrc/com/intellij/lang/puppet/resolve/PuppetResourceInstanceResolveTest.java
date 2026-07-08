package com.intellij.lang.puppet.resolve;

import com.intellij.lang.puppet.PuppetTestUtil;
import com.intellij.lang.puppet.psi.references.PuppetResourceInstanceReference;
import com.intellij.lang.puppet.psi.references.PuppetVariableReference;
import com.intellij.testFramework.TestFrameworkUtil;
import junit.framework.TestSuite;

import static com.intellij.lang.puppet.resolve.PuppetResolveTestCase.ResultType.ONE;
import static com.intellij.lang.puppet.resolve.PuppetResolveTestCase.ResultType.ZERO;
import static com.intellij.lang.puppet.resolve.PuppetResolveTestCase.TestType.CARET;

public class PuppetResourceInstanceResolveTest extends PuppetResolveTestCase {
  @Override
  protected String getBasePath() {
    return super.getBasePath() + "/resource_instance";
  }

  public void testReferenceQToName() {
    doTest(CARET, ONE, PuppetResourceInstanceReference.class);
  }

  public void testReferenceNameToQ() {
    doTest(CARET, ONE, PuppetResourceInstanceReference.class);
  }

  public void testReferenceNameToVar() {
    doTest(CARET, ZERO, PuppetResourceInstanceReference.class);
  }

  public void testReferenceVarToName() {
    doTest(CARET, ZERO, PuppetVariableReference.class);
  }

  public static TestSuite suite() {
    return TestFrameworkUtil.flattenSuite(PuppetTestUtil.createTestSuiteForVersions(PuppetResourceInstanceResolveTest.class));
  }
}
