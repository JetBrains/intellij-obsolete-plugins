package com.jetbrains.plugins.compass;

import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public interface CompassSettingsPanel extends Disposable {
  boolean isModified(@Nullable CompassSettings compassSettings);

  boolean isConfigFilePathChanged(@Nullable CompassSettings compassSettings);

  boolean isExecutablePathChanged(@Nullable CompassSettings compassSettings);

  boolean isEnableOptionChanged(@Nullable CompassSettings compassSettings);

  void apply(@Nullable CompassSettings compassSettings);

  void reset(@Nullable CompassSettings compassSettings);

  @NotNull
  JComponent getComponent();

  @Override
  void dispose();
}
