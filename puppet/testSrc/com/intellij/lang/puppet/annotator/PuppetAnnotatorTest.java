package com.intellij.lang.puppet.annotator;

import com.intellij.lang.puppet.PuppetHighlightingTestCase;
import com.intellij.lang.puppet.PuppetTestUtil;
import com.intellij.testFramework.TestFrameworkUtil;
import junit.framework.TestSuite;

/**
 * Created by Alexandr.Evstigneev on 16.08.2016.
 * For v3 & v4 cases with same result
 */
public class PuppetAnnotatorTest extends PuppetHighlightingTestCase {
  @Override
  protected String getBasePath() {
    return "/annotator";
  }

  public void testIncompleteString() {
    doTest();
  }


  public static TestSuite suite() {
    return TestFrameworkUtil.flattenSuite(PuppetTestUtil.createTestSuiteForVersions(PuppetAnnotatorTest.class));
  }
}

