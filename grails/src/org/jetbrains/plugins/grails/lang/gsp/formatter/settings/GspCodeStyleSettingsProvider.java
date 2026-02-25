// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.formatter.settings;

import com.intellij.application.options.CodeStyleAbstractConfigurable;
import com.intellij.application.options.CodeStyleAbstractPanel;
import com.intellij.application.options.TabbedLanguageCodeStylePanel;
import com.intellij.openapi.options.Configurable;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.lang.gsp.GspLanguage;

/**
 * @author Rustam Vishnyakov
 */
public final class GspCodeStyleSettingsProvider extends CodeStyleSettingsProvider {
  @Override
  public @NotNull Configurable createSettingsPage(@NotNull CodeStyleSettings settings, @NotNull CodeStyleSettings originalSettings) {
    return new CodeStyleAbstractConfigurable(settings, originalSettings, GrailsBundle.message("language.gsp")) {

      @Override
      protected @NotNull CodeStyleAbstractPanel createPanel(@NotNull CodeStyleSettings settings) {
        return new GspCodeStyleSettingsMainPanel(getCurrentSettings(), settings);
      }

      @Override
      public String getHelpTopic() {
        return "reference.settingsdialog.codestyle.gsp";
      }
    };
  }

  private static class GspCodeStyleSettingsMainPanel extends TabbedLanguageCodeStylePanel {

    protected GspCodeStyleSettingsMainPanel(CodeStyleSettings currentSettings, CodeStyleSettings settings) {
      super(GspLanguage.INSTANCE, currentSettings, settings);
    }

    @Override
    protected void initTabs(CodeStyleSettings settings) {
      addIndentOptionsTab(settings);
    }
  }

  @Override
  public String getConfigurableDisplayName() {
    return GrailsBundle.message("language.gsp");
  }

  @Override
  public @NotNull String getConfigurableId() {
    return "preferences.sourceCode.GSP";
  }
}
