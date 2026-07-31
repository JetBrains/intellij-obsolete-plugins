package com.intellij.gwt.injected;

import com.intellij.codeInsight.injected.InjectedLanguageTestCase;
import com.intellij.grazie.spellcheck.GrazieSpellCheckingInspection;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.application.PathManager;
import com.intellij.spellchecker.SpellCheckerSeveritiesProvider;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author Konstantin Bulenkov
 */
public class GwtInjectedLanguageHighlightingTest extends InjectedLanguageTestCase {
  private String getFilePath(@NonNls final String ext) {
    return getTestName(false) + "." + ext;
  }

  @Override
  protected String getBasePath() {
    return PathManager.getHomePath() + "/plugins/GwtStudio/testData";
  }

  @NotNull
  @Override
  protected String getTestDataPath() {
    return getBasePath() + "/jsni/highlighting/";
  }

  public void testUnusedMethod() {
    configureByFile(getFilePath("java"));
    assertEmpty(doHighlighting(HighlightSeverity.WARNING));
  }

  public void testJavaScriptKeywordAsMethodName() {
    configureByFile(getFilePath("java"));
    assertEmpty(doHighlighting(HighlightSeverity.ERROR));
  }

  public void testTypoInJavaMethodReference() {
    configureByFile(getFilePath("java"));
    enableInspectionTool(new GrazieSpellCheckingInspection());
    assertEmpty(doHighlighting(SpellCheckerSeveritiesProvider.TYPO));
  }
}
