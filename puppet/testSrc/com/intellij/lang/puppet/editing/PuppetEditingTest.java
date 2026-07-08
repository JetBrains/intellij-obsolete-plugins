package com.intellij.lang.puppet.editing;

import com.intellij.application.options.CodeStyle;
import com.intellij.lang.puppet.PuppetFileType;
import com.intellij.lang.puppet.PuppetTestUtil;
import com.intellij.lang.puppet.project.PuppetTestCase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.testFramework.TestFrameworkUtil;
import junit.framework.TestSuite;

/**
 * @author Anna Bulenkova
 */
public class PuppetEditingTest extends PuppetTestCase {
  private String myIndent;
  private String myDoubleIndent;

  public void testEnterBeforeNewLine() {
    doTestTyping("<caret>file", "\nfile", '\n');
  }

  public void testEnterAfterCurlyBrace() {
    doTestTyping("class unix {<caret>", "class unix {" + "\n" + myIndent + "\n}", '\n');
  }

  public void testEnterAfterColon() {
    doTestTyping("file {\n  '/etc/passwd':<caret>\n}", "file {\n  '/etc/passwd':\n" + myDoubleIndent + "\n}", '\n');
  }

  public void testEnterInLineComment() {
    doTestTyping("#comment<caret>", "#comment\n", '\n');
  }

  public void testEnterInBlockComment() {
    doTestTyping("/*<caret>", "/*\n", '\n');
  }

  public void testEnterAfterBlockComment() {
    doTestTyping("/*\ncomment\n*/<caret>", "/*\ncomment\n*/\n", '\n');
  }

  public void testQuote() {
    doTestTyping("", "\"\"", (char)34);
  }

  public void testQuoteBeforeIdentifier() {
    doTestTyping("literal", "\"literal", (char)34);
  }

  public void testQuoteBeforeQuotedString() {
    doTestTyping("\"string\"", "\"\"\"string\"", (char)34);
  }

  public void testBackSpaceInBraces() {
    doTestTyping("{<caret>}", "", '\b');
  }

  public void testBackSpaceInQuotes() {
    doTestTyping("\"<caret>\"", "", '\b');
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    final Project project = myFixture.getProject();
    CodeStyleSettings currentSettings = CodeStyle.getSettings(project);
    assertNotNull(currentSettings);
    CommonCodeStyleSettings.IndentOptions indentOptions = currentSettings.getIndentOptions(PuppetFileType.INSTANCE);
    assertNotNull(indentOptions);
    myIndent = StringUtil.repeatSymbol(' ', indentOptions.INDENT_SIZE);
    myDoubleIndent = myIndent + myIndent;
  }

  private void doTestTyping(final String source, String expected, final char character) {
    myFixture.configureByText(PuppetFileType.INSTANCE, source);
    myFixture.type(character);
    myFixture.checkResult(expected, true);
  }

  public static TestSuite suite() {
    return TestFrameworkUtil.flattenSuite(PuppetTestUtil.createTestSuiteForVersions(PuppetEditingTest.class));
  }
}

