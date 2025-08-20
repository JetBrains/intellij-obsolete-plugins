package com.intellij.frameworks.play;

import com.intellij.lang.xml.XMLParserDefinition;
import com.intellij.play.language.PlayFileViewProviderFactory;
import com.intellij.play.language.PlayLanguage;
import com.intellij.play.language.PlayParserDefinition;
import com.intellij.psi.LanguageFileViewProviders;
import com.intellij.testFramework.ParsingTestCase;
import org.jetbrains.plugins.groovy.lang.parser.GroovyParserDefinition;

import java.io.File;

public class PlayParserTest extends ParsingTestCase {
  public PlayParserTest() {
    super("parser", "html", new PlayParserDefinition(), new GroovyParserDefinition());
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    addExplicitExtension(LanguageFileViewProviders.INSTANCE, PlayLanguage.INSTANCE, new PlayFileViewProviderFactory());
    registerParserDefinition(new XMLParserDefinition());
  }

  @Override
  protected String getTestDataPath() {
    return new File("src/test/testData").getAbsolutePath();
  }

  public void testAction() {
    doTest(true);
  }

  public void testExpression() {
    doTest(true);
  }

  public void testTags() {
    doTest(true);
  }

  public void testTagWithAction() {
    doTest(true);
  }

  public void testTagWithChildren() {
    doTest(true);
  }

  public void testTagWithChildren2() {
    doTest(true);
  }

  public void testTagWithChildren3() {
    doTest(true);
  }

  public void testSimpleTag() {
    doTest(true);
  }

  public void testEndTag() {
    doTest(true);
  }

  public void testSetTag() {
    doTest(true);
  }

  public void testEmptyTagError() {
    doTest(true);
  }

  public void testUnclosedTagError() {
    doTest(true);
  }

  public void testIncorrectTagClosingError() {
    doTest(true);
  }

  public void testIncorrectClosingTag() {
    doTest(true);
  }
  public void testActionAttributes() {
    doTest(true);
  }

  public void testStringExpression() {
    doTest(true);
  }

  public void testQuickCloseTag() {
    doTest(true);
  }

  @Override
  protected boolean checkAllPsiRoots() {
    return false;
  }
}
