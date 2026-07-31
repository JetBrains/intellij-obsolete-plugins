package com.intellij.gwt.css;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.gwt.references.GwtCodeInsightTestCase;
import com.intellij.testFramework.TestDataPath;

@TestDataPath("$CONTENT_ROOT/../testData/css/typing/")
public class GwtCssTypingTest extends GwtCodeInsightTestCase {
  public void testBraceTyped() {
    myCodeInsightFixture.configureByFile("client/styleRef.ui.xml");
    myCodeInsightFixture.type('{');
    myCodeInsightFixture.checkResultByFile("client/styleRef_after.ui.xml");
  }

  public void testHonorInsertPairBracketOption() {
    myCodeInsightFixture.configureByFile("client/styleRef.ui.xml");
    final CodeInsightSettings settings = CodeInsightSettings.getInstance();
    boolean oldValue = settings.AUTOINSERT_PAIR_BRACKET;
    try {
      settings.AUTOINSERT_PAIR_BRACKET = false;
      myCodeInsightFixture.type('{');
    }
    finally {
      settings.AUTOINSERT_PAIR_BRACKET = oldValue;
    }
    myCodeInsightFixture.checkResultByFile("client/styleRefNoPair_after.ui.xml");
  }

  public void testBackspace() {
    myCodeInsightFixture.configureByFile("client/styleRef_after.ui.xml");
    myCodeInsightFixture.type('\b');
    myCodeInsightFixture.checkResultByFile("client/styleRef.ui.xml");
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "css/typing";
  }
}
