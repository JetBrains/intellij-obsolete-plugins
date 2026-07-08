package com.intellij.lang.puppet.folding;

import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.PuppetTestUtil;
import com.intellij.lang.puppet.project.PuppetTestCase;
import com.intellij.testFramework.TestFrameworkUtil;
import junit.framework.TestSuite;

public class PuppetFoldingTest extends PuppetTestCase {

  public void testFolding() {
    doLevelDependentTest();
  }

  public void testRuby18718() {doDefaultTest();}

  public void testResourceLikeClass() {doDefaultTest();}

  protected String getResultSuffix() {
    return getLanguageVersion() == PuppetLanguage.Version.PUPPET_3 ? ".txt3" : ".txt4";
  }

  private void doLevelDependentTest() {
    doTest(getResultSuffix());
  }

  private void doDefaultTest() {
    doTest(".txt");
  }

  private void doTest(String suffix) {
    String baseFileName = getTestDataPath() + "folding/" + getTestName(true);
    String sourceFileName = baseFileName + ".pp";
    String verificationFileName = baseFileName + suffix;
    myFixture.testFoldingWithCollapseStatus(verificationFileName, sourceFileName);
  }

  public static TestSuite suite() {
    return TestFrameworkUtil.flattenSuite(PuppetTestUtil.createTestSuiteForVersions(PuppetFoldingTest.class));
  }
}
