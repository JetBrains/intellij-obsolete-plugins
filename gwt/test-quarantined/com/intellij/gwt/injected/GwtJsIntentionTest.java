package com.intellij.gwt.injected;

import com.intellij.gwt.GwtTestCase;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.intellij.idea.lang.javascript.intention.JSIntentionBundle;

public class GwtJsIntentionTest extends LightJavaCodeInsightFixtureTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.setTestDataPath(GwtTestCase.getGwtTestDataPath() + FileUtil.toSystemDependentName("jsni/intentions/"));
  }

  public void testMergeParallelIfs() {
    doIntentionTest(JSIntentionBundle.message("trivialif.merge-parallel-ifs.display-name"));
  }

  public void testSplitDeclarationAndInitialization() {
    doIntentionTest(JSIntentionBundle.message("initialization.split-declaration-and-initialization.display-name"));
  }

  private void doIntentionTest(String familyName) {
    myFixture.configureByFile(getTestName(false) + ".java");
    myFixture.launchAction(myFixture.findSingleIntention(familyName));
    myFixture.checkResultByFile(getTestName(false) + "_after.java");
  }
}
