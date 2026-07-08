package com.intellij.lang.puppet.resolve;

import com.intellij.lang.puppet.OnVersion;
import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.PuppetTestUtil;
import com.intellij.lang.puppet.psi.references.PuppetFunctionReference;
import com.intellij.testFramework.TestFrameworkUtil;
import junit.framework.TestSuite;

import static com.intellij.lang.puppet.resolve.PuppetResolveTestCase.ResultType.ONE;
import static com.intellij.lang.puppet.resolve.PuppetResolveTestCase.TestType.CARET;

@OnVersion(PuppetLanguage.Version.PUPPET_4)
public class PuppetFunctionResolveTest extends PuppetResolveTestCase {
  @Override
  protected String getBasePath() {
    return super.getBasePath() + "/function";
  }

  public void testInFile() {
    doTest(CARET, ONE, PuppetFunctionReference.class);
  }

  public void testFunctionReturnType() {doTest();}

  public void testExternalFunction() {
    configureByManifest("puppetFunctionDefinition.code");
    doTest(CARET, ONE, PuppetFunctionReference.class);
  }

  public void testTopLevelFunction() {
    configureByManifest("functions", "puppetFunctionDefinition.code");
    doTest(CARET, ONE, PuppetFunctionReference.class);
  }

  public static TestSuite suite() {
    return TestFrameworkUtil.flattenSuite(PuppetTestUtil.createTestSuiteForVersions(PuppetFunctionResolveTest.class));
  }
}
