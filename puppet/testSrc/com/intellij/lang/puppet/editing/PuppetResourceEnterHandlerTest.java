package com.intellij.lang.puppet.editing;

import com.intellij.lang.puppet.PuppetTestUtil;
import com.intellij.lang.puppet.project.PuppetTestCase;
import com.intellij.testFramework.TestFrameworkUtil;
import junit.framework.TestSuite;

public class PuppetResourceEnterHandlerTest extends PuppetTestCase {
  public void testGeneral() {
    doTest();
  }

  public void testTrailingLeft() {
    doTest();
  }

  public void testTrailingRight() {
    doTest();
  }

  public void testTrailingBoth() {
    doTest();
  }

  public void testOnAnotherLine() {
    doTest();
  }

  public void testOnAnotherLine2() {
    doTest();
  }

  public void testOnAnotherLine3() {
    doTest();
  }

  public void testFirstLineBefore1() {
    doTest();
  }

  public void testFirstLineBefore2() {
    doTest();
  }

  public void testNested1() {
    doTest();
  }

  public void testNested2() {
    doTest();
  }

  public void testNested3() {
    doTest();
  }

  public void doTest() {
    final String testName = getTestName(true);
    myFixture.configureByFile(testName + ".pp");
    myFixture.type('\n');
    myFixture.checkResultByFile(testName + "_after.pp");
  }

  @Override
  protected String getBasePath() {
    return "/editing/resourceEnterHandler";
  }

  public static TestSuite suite() {
    return TestFrameworkUtil.flattenSuite(PuppetTestUtil.createTestSuiteForVersions(PuppetResourceEnterHandlerTest.class));
  }
}
