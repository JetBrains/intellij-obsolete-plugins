// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.groovy.grails;

import com.intellij.codeInsight.actions.MultiCaretCodeInsightAction;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

import static org.jetbrains.plugins.groovy.grails.GrailsTestUtil.getTestRootPath;

public class GrailsActionsTest extends LightJavaCodeInsightFixtureTestCase {

  @Override
  protected String getTestDataPath() {
    return getTestRootPath("/testdata/grails/actions/");
  }

  private void performMultiCaretCodeInsightAction(final String actionId) {
    MultiCaretCodeInsightAction action = (MultiCaretCodeInsightAction) ActionManager.getInstance().getAction(actionId);
    action.actionPerformedImpl(myFixture.getProject(), myFixture.getEditor());
  }

  public void testGspLineComment() { doTest(IdeActions.ACTION_COMMENT_LINE); }
  public void testGspLineUncomment() { doTest(IdeActions.ACTION_COMMENT_LINE); }
  public void testGspBlockComment() { doTest(IdeActions.ACTION_COMMENT_BLOCK); }

  private void doTest(String actionId) {
    myFixture.configureByFile(getTestName(false) + ".gsp");
    performMultiCaretCodeInsightAction(actionId);
    myFixture.checkResultByFile(getTestName(false) + "_after.gsp");
  }

}