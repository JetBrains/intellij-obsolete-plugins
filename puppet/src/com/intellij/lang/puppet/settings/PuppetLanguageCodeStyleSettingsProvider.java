package com.intellij.lang.puppet.settings;

import com.intellij.application.options.IndentOptionsEditor;
import com.intellij.application.options.SmartIndentOptionsEditor;
import com.intellij.lang.Language;
import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import org.jetbrains.annotations.NotNull;

/**
 * @author Anna Bulenkova
 */
public class PuppetLanguageCodeStyleSettingsProvider extends LanguageCodeStyleSettingsProvider {
  @Override
  public @NotNull Language getLanguage() {
    return PuppetLanguage.INSTANCE;
  }

  @Override
  protected void customizeDefaults(@NotNull CommonCodeStyleSettings commonSettings,
                                   @NotNull CommonCodeStyleSettings.IndentOptions indentOptions) {
    indentOptions.INDENT_SIZE = 2;
    indentOptions.CONTINUATION_INDENT_SIZE = 2;
    indentOptions.TAB_SIZE = 2;

    commonSettings.BLOCK_COMMENT_AT_FIRST_COLUMN = false;
    commonSettings.LINE_COMMENT_AT_FIRST_COLUMN = false;
  }

  @Override
  public String getCodeSample(@NotNull SettingsType settingsType) {
    return PuppetBundle.message("language.settings.code.style.sample");
  }

  @Override
  public IndentOptionsEditor getIndentOptionsEditor() {
    return new SmartIndentOptionsEditor();
  }
}
