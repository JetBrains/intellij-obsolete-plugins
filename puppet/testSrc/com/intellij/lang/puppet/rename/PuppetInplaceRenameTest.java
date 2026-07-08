package com.intellij.lang.puppet.rename;

import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateState;
import com.intellij.idea.IJIgnore;
import com.intellij.lang.puppet.OnVersion;
import com.intellij.lang.puppet.PuppetFileType;
import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.PuppetTestUtil;
import com.intellij.lang.puppet.ide.refactoring.PuppetMemberInplaceRenameHandler;
import com.intellij.lang.puppet.ide.refactoring.PuppetMemberInplaceRenamer;
import com.intellij.lang.puppet.project.PuppetTestCase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageEditorUtil;
import com.intellij.testFramework.TestFrameworkUtil;
import com.intellij.testFramework.fixtures.CodeInsightTestUtil;
import junit.framework.TestSuite;
import org.jetbrains.annotations.NotNull;

@IJIgnore(issue = "RUBY-34992")
public class PuppetInplaceRenameTest extends PuppetTestCase {

  @Override
  protected String getBasePath() {
    return "rename";
  }

  public void testVariableRename() {
    doTest("newName");
  }

  public void testVariableIncorrectRename() {
    doTest("NewName");
  }

  public void testClassRename() {
    doTest();
  }

  public void testNamespaceRename() {
    doTest();
  }

  public void testTypeRename() {
    doTest();
  }

  public void testSynonimsRenameSynonim() {doTest("newname");}

  public void testSynonimsRenameUsage() {doTest("newname");}

  public void testSynonimsRenameTarget() {doTest("newname");}

  public void testSmartResourceInstanceRename() {
    doTest("/smart/name");
  }

  public void testResourceInstanceRename() {
    doTest("newName");
  }

  public void testSmartResourceInstanceRenameWithQuote() {
    doTest("resource'name");
  }

  public void testSmartResourceInstanceRenameWithDoubleQuote() {
    doTest("resource\"name");
  }

  public void testSmartResourceInstanceRenameWithEscapedQuote() {
    doTest("resource\\'name");
  }

  public void testBarewordResourceInstanceRename() {
    doTest("newName");
  }

  public void testCancelledLongerRename() {doCancelledTest("superlongname");}

  public void testCancelledShorterRename() {doCancelledTest("bom");}

  @OnVersion(PuppetLanguage.Version.PUPPET_4)
  public void testFunctionRename() {
    doTest();
  }

  private void doTest() {
    doTest("NewName");
  }

  private void doTest(@NotNull String newName) {
    String testName = getTestName(true);
    String testFileName = testName + "." + PuppetFileType.DEFAULT_EXTENSION;
    String testAnswerFileName = testName + ".txt";

    myFixture.configureByFile(testFileName);
    CodeInsightTestUtil.doInlineRename(new PuppetMemberInplaceRenameHandler(), newName, myFixture);
    myFixture.checkResultByFile(testAnswerFileName);
  }

  public void doCancelledTest(String initialName) {
    String testName = getTestName(true);
    String answersName = testName + ".txt";
    String sourceFileName = "cancelledRename.pp";
    myFixture.configureByFile(sourceFileName);

    Editor editor = myFixture.getEditor();
    String originalText = editor.getDocument().getText();

    PsiElement elementAtCaret = myFixture.getElementAtCaret();
    Project project = getProject();
    TemplateManagerImpl.setTemplateTesting(getTestRootDisposable());
    PuppetMemberInplaceRenamer renamer = new PuppetMemberInplaceRenamer((PsiNamedElement)elementAtCaret, null, editor, initialName,
                                                                        ((PsiNamedElement)elementAtCaret).getName());
    renamer.performInplaceRename();
    editor = InjectedLanguageEditorUtil.getTopLevelEditor(editor);
    TemplateState state = TemplateManagerImpl.getTemplateState(editor);
    myFixture.checkResultByFile(answersName);
    state.gotoEnd(true);

    myFixture.checkResult(originalText);
  }

  public static TestSuite suite() {
    return TestFrameworkUtil.flattenSuite(PuppetTestUtil.createTestSuiteForVersions(PuppetInplaceRenameTest.class));
  }
}
