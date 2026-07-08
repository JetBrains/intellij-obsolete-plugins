package com.intellij.lang.puppet.formatter;

import com.intellij.application.options.TabbedLanguageCodeStylePanel;
import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.psi.codeStyle.CodeStyleSettings;

/**
 * @author Anna Bulenkova
 */
public class PuppetCodeStylePanel extends TabbedLanguageCodeStylePanel {
  protected PuppetCodeStylePanel(CodeStyleSettings currentSettings, CodeStyleSettings settings) {
    super(PuppetLanguage.INSTANCE, currentSettings, settings);
  }

  @Override
  protected void initTabs(final CodeStyleSettings settings) {
    addIndentOptionsTab(settings);
  }
}