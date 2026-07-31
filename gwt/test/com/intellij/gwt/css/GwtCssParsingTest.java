package com.intellij.gwt.css;

import com.intellij.gwt.GwtTestCase;
import com.intellij.gwt.clientBundle.css.language.GwtCssLanguage;
import com.intellij.gwt.clientBundle.css.language.GwtCssParserDefinition;
import com.intellij.gwt.clientBundle.css.language.GwtCssTreeElementFactory;
import com.intellij.lang.LanguageASTFactory;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.css.CSSLanguage;
import com.intellij.lang.css.CSSParserDefinition;
import com.intellij.psi.css.impl.CssTreeElementFactory;
import com.intellij.testFramework.ParsingTestCase;
import org.jetbrains.annotations.NotNull;

public class GwtCssParsingTest extends ParsingTestCase {
  public GwtCssParsingTest() {
    super("css/parsing", "css", new GwtCssParserDefinition(), new CSSParserDefinition());
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    addExplicitExtension(LanguageASTFactory.INSTANCE, GwtCssLanguage.GWT_CSS_LANGUAGE, new GwtCssTreeElementFactory());
    addExplicitExtension(LanguageASTFactory.INSTANCE, CSSLanguage.INSTANCE, new CssTreeElementFactory());
  }

  @Override
  public void configureFromParserDefinition(@NotNull ParserDefinition definition, String extension) {
    super.configureFromParserDefinition(definition, extension);
  }

  public void testDef() {
    doTest();
  }

  public void testEval() {
    doTest();
  }

  public void testExternal() {
    doTest();
  }

  public void testIfStatement() {
    doTest();
  }

  public void testIfStatementJavaCondition() {
    doTest();
  }

  public void testIfElseStatement() {
    doTest();
  }

  public void testIfElseIfStatement() {
    doTest();
  }

  public void testNoFlip() {
    doTest();
  }

  public void testSprite() {
    doTest();
  }

  public void testUrl() {
    doTest();
  }

  private void doTest() {
    doTest(true);
  }

  @Override
  protected String getTestDataPath() {
    return GwtTestCase.getGwtTestDataPath();
  }
}
