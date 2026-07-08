package com.intellij.lang.puppet.highlighting;

import com.intellij.lang.puppet.OnVersion;
import com.intellij.lang.puppet.PuppetHighlightingTestCase;
import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.PuppetTestUtil;
import com.intellij.lang.puppet.ide.inspections.Puppet3DeprecationsInspection;
import com.intellij.lang.puppet.ide.inspections.Puppet4DeprecationsInspection;
import com.intellij.lang.puppet.ide.inspections.PuppetMultipleHashSetParamsPerResourceInstanceInspection;
import com.intellij.lang.puppet.ide.inspections.PuppetUnresolvedInspection;
import com.intellij.testFramework.TestFrameworkUtil;
import junit.framework.TestSuite;

public class PuppetInspectionHighlightingTest extends PuppetHighlightingTestCase {

  @Override
  protected String getBasePath() {
    return "/highlighting";
  }

  @OnVersion(PuppetLanguage.Version.PUPPET_4)
  public void testMultipleHashSetParamInspection() {
    myFixture.enableInspections(PuppetMultipleHashSetParamsPerResourceInstanceInspection.class);
    doTest();
  }

  @OnVersion(PuppetLanguage.Version.PUPPET_4)
  public void testPuppet4LanguageDeprecationsInspection() {
    myFixture.enableInspections(Puppet4DeprecationsInspection.class);
    doTest();
  }

  @OnVersion(PuppetLanguage.Version.PUPPET_4)
  public void testRuby19384() {
    myFixture.enableInspections(PuppetUnresolvedInspection.class);
    doTest();
  }

  @OnVersion(PuppetLanguage.Version.PUPPET_3)
  public void testPuppet3LanguageDeprecationsInspection() {
    myFixture.enableInspections(Puppet3DeprecationsInspection.class);
    doTest();
  }

  public static TestSuite suite() {
    return TestFrameworkUtil.flattenSuite(PuppetTestUtil.createTestSuiteForVersions(PuppetInspectionHighlightingTest.class));
  }
}
