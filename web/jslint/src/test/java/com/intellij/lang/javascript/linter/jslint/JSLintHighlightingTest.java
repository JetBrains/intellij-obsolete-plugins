package com.intellij.lang.javascript.linter.jslint;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class JSLintHighlightingTest extends BasePlatformTestCase {

  @Override
  protected String getTestDataPath() {
    return getBasePath() + getTestDataSubdir();
  }

  protected String getBasePath() {
    return "src/test/testData/highlighting";
  }

  protected String getTestDataSubdir() {
    return "";
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    myFixture.enableInspections(new JSLintInspection());
  }

  public void testAssignment() {
    JSLintState state = new JSLintState.Builder()
      .setOptionsState(
        new JSLintOptionsState.Builder()
          .put(JSLintOption.GLOBALS, "foo:true")
          .put(JSLintOption.WHITE, true)
          .build())
      .build();

    JSLintConfiguration configuration = JSLintConfiguration.getInstance(getProject());
    configuration.setExtendedState(true, state);
    doTest();

    state = new JSLintState.Builder()
      .setOptionsState(
        new JSLintOptionsState.Builder()
          .put(JSLintOption.GLOBALS, "foo:true, bar")
          .put(JSLintOption.WHITE, true)
          .put(JSLintOption.EVAL, true)
          .build())
      .build();

    configuration = JSLintConfiguration.getInstance(getProject());
    configuration.setExtendedState(true, state);
    doTestAfter();
  }

  public void testMultiVar() {
    JSLintState state = new JSLintState.Builder()
      .setOptionsState(
        new JSLintOptionsState.Builder()
          .put(JSLintOption.SINGLE, true)
          .build())
      .build();

    JSLintConfiguration configuration = JSLintConfiguration.getInstance(getProject());
    configuration.setExtendedState(true, state);
    doTest();

    state = new JSLintState.Builder()
      .setOptionsState(
        new JSLintOptionsState.Builder()
          .put(JSLintOption.SINGLE, false)
          .build())
      .build();

    configuration = JSLintConfiguration.getInstance(getProject());
    configuration.setExtendedState(true, state);
    doTestAfter();
  }

  private void doTest() {
    myFixture.testHighlighting(true, false, false, getTestName(true) + ".js");
  }

  private void doTestAfter() {
    myFixture.testHighlighting(true,false, false,  getTestName(true) + "_after.js");
  }
}
