package com.intellij.lang.puppet.editing;

import com.intellij.codeInsight.actions.MultiCaretCodeInsightAction;
import com.intellij.lang.puppet.PuppetTestUtil;
import com.intellij.lang.puppet.project.PuppetTestCase;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.testFramework.TestFrameworkUtil;
import junit.framework.TestSuite;
import org.jetbrains.annotations.NotNull;

/**
 * @author Anna Bulenkova
 */

public class PuppetCommenterTest extends PuppetTestCase {

  public void testComment() {
    doTest(CommentType.LINE);
  }
  public void testUncomment() {
    doTest(CommentType.LINE);
  }

  public void testCommentNestedStatement() {
    doTest(CommentType.LINE);
  }

  public void testCommentNestedBlockStatement() {
    doTest(CommentType.BLOCK);
  }

  public void testUncommentNestedBlockStatement() {
    doTest(CommentType.BLOCK);
  }

  public void testUncommentNestedBlockStatementNoSpace() {
    doTest(CommentType.BLOCK);
  }

  private void doTest(@NotNull CommentType commentType) {
    myFixture.configureByFile(getTestName(true) + "_before.pp");
    MultiCaretCodeInsightAction action = (MultiCaretCodeInsightAction) ActionManager.getInstance().getAction(commentType.actionId);
    action.actionPerformedImpl(getProject(), myFixture.getEditor());
    myFixture.checkResultByFile(getTestName(true) + "_after.pp");
  }

  @Override
  protected String getBasePath() {
    return "/commenter/";
  }

  public static TestSuite suite() {
    return TestFrameworkUtil.flattenSuite(PuppetTestUtil.createTestSuiteForVersions(PuppetCommenterTest.class));
  }

  private enum CommentType {
    LINE(IdeActions.ACTION_COMMENT_LINE),
    BLOCK(IdeActions.ACTION_COMMENT_BLOCK);

    public final @NotNull String actionId;

    CommentType(@NotNull String actionId) {
      this.actionId = actionId;
    }
  }
}
