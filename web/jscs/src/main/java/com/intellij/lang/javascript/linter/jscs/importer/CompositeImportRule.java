package com.intellij.lang.javascript.linter.jscs.importer;

import com.google.gson.JsonElement;
import com.intellij.lang.javascript.formatter.JSCodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;

/**
 * @author Irina.Chernushina on 10/22/2015.
 */
public class CompositeImportRule extends ImportRule {
  private final ImportRule[] myDelegates;

  public CompositeImportRule(ImportRule... delegates) {
    myDelegates = delegates;
  }

  @Override
  protected boolean applyImpl(JsonElement element, CommonCodeStyleSettings settings, JSCodeStyleSettings jsCodeStyleSettings) {
    boolean apply = false;
    for (ImportRule delegate : myDelegates) {
      apply |= delegate.applyImpl(element, settings, jsCodeStyleSettings);
    }
    return apply;
  }
}
