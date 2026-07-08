package com.intellij.lang.puppet.formatter;

import com.intellij.application.options.CodeStyleAbstractConfigurable;
import com.intellij.application.options.CodeStyleAbstractPanel;
import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.openapi.options.Configurable;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider;
import org.jetbrains.annotations.NotNull;

public class PuppetCodeStyleSettingsProvider extends CodeStyleSettingsProvider {
  @Override
  public @NotNull Configurable createSettingsPage(@NotNull CodeStyleSettings settings, @NotNull CodeStyleSettings originalSettings) {
    return new CodeStyleAbstractConfigurable(settings, originalSettings, PuppetBundle.PUPPET_UI_NAME) {
      @Override
      protected @NotNull CodeStyleAbstractPanel createPanel(@NotNull CodeStyleSettings settings) {
        return new PuppetCodeStylePanel(getCurrentSettings(), settings);
      }

      @Override
      public String getHelpTopic() {
        return "reference.settingsdialog.codestyle.puppet";
      }
    };
  }

  @Override
  public String getConfigurableDisplayName() {
    return PuppetBundle.message("language.name");
  }

  @Override
  public @NotNull String getConfigurableId() {
    return "preferences.sourceCode.Puppet";
  }
}