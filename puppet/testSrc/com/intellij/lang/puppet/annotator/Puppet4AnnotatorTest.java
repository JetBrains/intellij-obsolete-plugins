package com.intellij.lang.puppet.annotator;

import com.intellij.lang.puppet.OnVersion;
import com.intellij.lang.puppet.PuppetHighlightingTestCase;
import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.PuppetTestUtil;
import com.intellij.testFramework.TestFrameworkUtil;
import junit.framework.TestSuite;

/**
 * Created by Alexandr.Evstigneev on 16.08.2016.
 * v4 specific cases
 */
@OnVersion(PuppetLanguage.Version.PUPPET_4)
public class Puppet4AnnotatorTest extends PuppetHighlightingTestCase {
  @Override
  protected String getBasePath() {
    return "/annotator/v4";
  }

  public void testEscapedQuote() {
    doTest();
  }

  public void testEscapedQuoteOnIncompleteString() {
    doTest();
  }

  public void testPerExpressionDefault() {
    doTest();
  }

  public static TestSuite suite() {
    return TestFrameworkUtil.flattenSuite(PuppetTestUtil.createTestSuiteForVersions(Puppet4AnnotatorTest.class));
  }
}
