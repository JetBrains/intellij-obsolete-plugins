package com.intellij.lang.puppet.resolve;

import com.intellij.lang.puppet.OnVersion;
import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.PuppetTestUtil;
import com.intellij.testFramework.TestFrameworkUtil;
import junit.framework.TestSuite;

import static com.intellij.lang.puppet.resolve.PuppetResolveTestCase.TestType.ALL;

/**
 * Test for different mixed situations
 */
@OnVersion(PuppetLanguage.Version.PUPPET_4)
public class PuppetMixedResolveTest extends PuppetResolveTestCase {
  @Override
  protected String getBasePath() {
    return super.getBasePath() + "/mixed";
  }

  public void testRuby19014() {doTest(ALL);}

  public void testRegularNames() {doTest(ALL);}

  public void testQuotedText() {doTest(ALL);}

  public void testCapitalizedNames() {doTest(ALL);}

  public void testResolveToNamespace() {doTest(ALL);}

  public void testResolveToClassAndType() {doTest(ALL);}

  public static TestSuite suite() {
    return TestFrameworkUtil.flattenSuite(PuppetTestUtil.createTestSuiteForVersions(PuppetMixedResolveTest.class));
  }
}
