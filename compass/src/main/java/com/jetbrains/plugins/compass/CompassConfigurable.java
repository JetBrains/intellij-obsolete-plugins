package com.jetbrains.plugins.compass;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

public class CompassConfigurable implements SearchableConfigurable, Configurable.NoScroll {
  @Nullable private final Module myModule;
  @Nullable private final CompassSassExtension myCompassSassExtension;
  private final boolean myFullMode;
  @Nullable private final CompassSettings myCompassSettings;
  @Nullable private CompassSettingsPanel myPanel;

  public CompassConfigurable(@Nullable final Module module, @Nullable CompassSassExtension compassSassExtension, boolean fullMode) {
    myModule = module;
    myCompassSassExtension = compassSassExtension;
    myFullMode = fullMode;
    myCompassSettings = module != null ? CompassSettings.getInstance(module) : null;
  }

  @NotNull
  @Override
  public String getId() {
    return CompassConfigurableProvider.HELP_TOPIC;
  }

  @Nls
  @Override
  public String getDisplayName() {
    return "Compass";
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    return "reference.settings.project.settings.compass.support";
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    if (myPanel == null) {
      if (myModule != null && myCompassSettings != null) {
        myPanel = createCompassSettingsPanel(myModule, myFullMode);
      }
    }
    return myPanel != null ? myPanel.getComponent() : null;
  }

  @Override
  public boolean isModified() {
    return myPanel != null && myPanel.isModified(myCompassSettings);
  }

  @Override
  public void apply() {
    if (myPanel != null) {
      final boolean executablePathChanged = myPanel.isExecutablePathChanged(myCompassSettings);
      final boolean enableOptionChanged = myPanel.isEnableOptionChanged(myCompassSettings);
      final boolean configPathChanged = myPanel.isConfigFilePathChanged(myCompassSettings);
      myPanel.apply(myCompassSettings);

      if (myModule != null && myCompassSettings != null) {
        if (!myCompassSettings.isCompassSupportEnabled()) {
          CompassUtil.removeCompassLibraryIfNeeded(myModule);
        }

        if (enableOptionChanged || configPathChanged || executablePathChanged) {
          if (myCompassSassExtension != null) {
            myCompassSassExtension.stopActivity(myModule);
            myCompassSettings.setImportPaths(Collections.emptyList());
            if (myCompassSassExtension.isAvailableInModule(myModule)) {
              myCompassSassExtension.startActivity(myModule);
            }
          }
        }
      }
    }
  }

  @NotNull
  protected CompassSettingsPanel createCompassSettingsPanel(@NotNull Module module, boolean fullMode) {
    List<String> variants = ApplicationManager.getApplication().isUnitTestMode() 
                            ? Collections.emptyList()
                            : CompassUtil.getExecutableFilesVariants();
    return new CompassSettingsPanelImpl(module, variants, CompassUtil.getConfigFileVariants(module), fullMode);
  }

  @Override
  public void reset() {
    if (myPanel != null) {
      myPanel.reset(myCompassSettings);
    }
  }

  @Override
  public void disposeUIResources() {
    if (myPanel != null) {
      Disposer.dispose(myPanel);
    }
    myPanel = null;
  }
}
