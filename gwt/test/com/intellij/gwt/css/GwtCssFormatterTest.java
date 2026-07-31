package com.intellij.gwt.css;

import com.intellij.gwt.GwtTestCase;
import com.intellij.gwt.clientBundle.css.language.GwtCssLanguage;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.formatter.FormatterTestCase;

public class GwtCssFormatterTest extends FormatterTestCase {
  public void testExternal() throws Exception {
    doTest();
  }

  public void testDefEvalUrl() throws Exception {
    doTest();
  }

  public void testSprite() throws Exception {
    doTest();
  }

  public void testNoFlip() throws Exception {
    doTest();
  }

  public void testIfStatement() throws Exception {
    doTest();
  }

  public void testIfElseIfStatement() throws Exception {
    doTest();
  }

  @Override
  protected String getBasePath() {
    return "/css/formatting";
  }

  @Override
  protected String getTestDataPath() {
    return GwtTestCase.getGwtTestDataPath();
  }

  @Override
  protected PsiFile createFileFromText(String text, String fileName, PsiFileFactory fileFactory) {
    return fileFactory.createFileFromText(fileName, GwtCssLanguage.GWT_CSS_LANGUAGE, text, true, false);
  }

  @Override
  protected String getFileExtension() {
    return "css";
  }
}
