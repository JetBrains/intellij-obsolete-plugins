package com.intellij.lang.javascript.linter.jscs.importer;

import com.google.gson.JsonElement;
import com.intellij.lang.javascript.formatter.JSCodeStyleSettings;
import com.intellij.lang.javascript.linter.jscs.config.JscsOption;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;

/**
 * @author Irina.Chernushina on 4/23/2015.
 */
public abstract class PairImportRule implements AppliedRule {
  private String myTextValue;
  private final StringBuilder myResultDescription;

  protected PairImportRule() {
    myResultDescription = new StringBuilder();
  }

  protected void append(final String text) {
    if (myResultDescription.length() > 0) myResultDescription.append("<br/>");
    myResultDescription.append(text);
  }

  public boolean apply(JsonElement element1,
                       JsonElement element2,
                       CommonCodeStyleSettings settings,
                       JSCodeStyleSettings jsCodeStyleSettings) {
    myTextValue = element1.toString() + " and " + element2.toString();
    return applyImpl(element1, element2, settings, jsCodeStyleSettings);
  }

  public abstract JscsOption getPairRule();
  protected abstract boolean applyImpl(JsonElement element1,
                                       JsonElement element2,
                                       CommonCodeStyleSettings settings,
                                       JSCodeStyleSettings jsCodeStyleSettings);

  @Override
  public String getTextValue() {
    return myTextValue;
  }

  @Override
  public String getResultDescription() {
    return myResultDescription.toString();
  }
}
