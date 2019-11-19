package com.jetbrains.plugins.compass.ruby;

import com.intellij.openapi.module.Module;
import com.jetbrains.plugins.compass.CompassConfigurable;
import com.jetbrains.plugins.compass.CompassSassExtension;
import com.jetbrains.plugins.compass.CompassSettingsPanel;
import com.jetbrains.plugins.compass.CompassUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompassConfigurableForRubyModule extends CompassConfigurable {
  public CompassConfigurableForRubyModule(@Nullable Module module, @Nullable CompassSassExtension extension, boolean fullMode) {
    super(module, extension, fullMode);
  }

  @Override
  @NotNull
  protected CompassSettingsPanel createCompassSettingsPanel(@NotNull Module module, boolean fullMode) {
    return new CompassSettingsPanelForRubyModule(module, CompassUtil.getConfigFileVariants(module), fullMode);
  }
}
