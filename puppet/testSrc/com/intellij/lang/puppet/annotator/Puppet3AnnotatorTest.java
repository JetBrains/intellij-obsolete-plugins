package com.intellij.lang.puppet.annotator;

import com.intellij.lang.puppet.OnVersion;
import com.intellij.lang.puppet.PuppetHighlightingTestCase;
import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.PuppetTestUtil;
import com.intellij.testFramework.TestFrameworkUtil;
import junit.framework.TestSuite;

/**
 * Created by Alexandr.Evstigneev on 16.08.2016.
 * v3 specific cases
 */
@OnVersion(PuppetLanguage.Version.PUPPET_3)
public class Puppet3AnnotatorTest extends PuppetHighlightingTestCase {
  @Override
  protected String getBasePath() {
    return "/annotator/v3";
  }

  public void testEscapedQuote() {
    doTest();
  }

  public void testPerExpressionDefault() {
    doTest();
  }

  public static TestSuite suite() {
    return TestFrameworkUtil.flattenSuite(PuppetTestUtil.createTestSuiteForVersions(Puppet3AnnotatorTest.class));
  }
}
