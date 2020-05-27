package com.intellij.lang.javascript.linter.jscs.importer;

import com.google.gson.JsonElement;
import com.intellij.lang.javascript.formatter.JSCodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;

/**
 * @author Irina.Chernushina on 4/21/2015.
 */
public abstract class ImportRule implements AppliedRule {
  private String myTextValue;
  private final StringBuilder myResultDescription;

  protected ImportRule() {
    myResultDescription = new StringBuilder();
  }

  protected void append(final String text) {
    if (myResultDescription.length() > 0) myResultDescription.append("<br/>");
    myResultDescription.append(text);
  }

  public boolean apply(JsonElement element, CommonCodeStyleSettings settings, JSCodeStyleSettings jsCodeStyleSettings) {
    myTextValue = element.toString();
    return applyImpl(element, settings, jsCodeStyleSettings);
  }

  protected abstract boolean applyImpl(JsonElement element, CommonCodeStyleSettings settings, JSCodeStyleSettings jsCodeStyleSettings);

  @Override
  public String getTextValue() {
    return myTextValue;
  }

  @Override
  public String getResultDescription() {
    return myResultDescription.toString();
  }
}
