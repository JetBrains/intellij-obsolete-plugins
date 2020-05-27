package com.intellij.lang.javascript.linter.jscs;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

/**
 * @author Irina.Chernushina on 10/10/2014.
 */
public class JscsConfigHighlightingTest extends BasePlatformTestCase {

  private JscsInspection myInspection;

  @Override
  protected String getTestDataPath() {
    return getBasePath();
  }

  protected String getBasePath() {
    return "src/test/testData/config/highlighting";
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myInspection = new JscsInspection();
    myFixture.enableInspections(myInspection);
  }

  @Override
  public void tearDown() throws Exception {
    try {
      myFixture.disableInspections(myInspection);
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      super.tearDown();
    }
  }

  public void testOne() {
    myFixture.testHighlighting(getTestName(true) + ".jscsrc");
  }

  public void testTwo() {
    myFixture.testHighlighting(getTestName(true) + ".jscsrc");
  }

  public void testThree() {
    myFixture.testHighlighting(getTestName(true) + ".jscs.json");
  }

  public void testFour() {
    myFixture.testHighlighting(getTestName(true) + ".jscsrc");
  }

  public void testFive() {
    myFixture.testHighlighting(getTestName(true) + ".jscsrc");
  }

  public void testRequirePaddingNewLinesAfterBlocks() {
    myFixture.testHighlighting(getTestName(true) + ".jscsrc");
  }

  public void testRequirePaddingNewLinesAfterBlocks2() {
    myFixture.testHighlighting(getTestName(true) + ".jscsrc");
  }

  public void testFileExtensions() {
    myFixture.testHighlighting(getTestName(true) + ".jscsrc");
  }

  public void testFileExtensions2() {
    myFixture.testHighlighting(getTestName(true) + ".jscsrc");
  }

  public void testrequireSpacesInFunctionExpression() {
    myFixture.testHighlighting(getTestName(true) + ".jscsrc");
  }

  public void testMaximumLineLength() {
    myFixture.testHighlighting(getTestName(true) + ".jscsrc");
  }

  public void testWeb16405() {
    myFixture.testHighlighting(getTestName(true) + ".jscsrc");
  }

  public void testQuote() {
    myFixture.testHighlighting(getTestName(true) + ".jscsrc");
  }

  public void testDeep() {
    myFixture.testHighlighting(getTestName(true) + ".jscsrc");
  }

  public void testWeb17727() {
    myFixture.testHighlighting(getTestName(true) + ".jscsrc");
  }

  public void testOptions022016() {
    myFixture.testHighlighting(getTestName(false) + ".jscsrc");
  }

  public void testRequireObjectKeysOnNewLine() {
    myFixture.testHighlighting(getTestName(true) + ".jscsrc");
  }
}
