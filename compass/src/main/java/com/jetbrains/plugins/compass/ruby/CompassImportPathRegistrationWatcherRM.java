package com.jetbrains.plugins.compass.ruby;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleServiceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.jetbrains.plugins.compass.CompassImportPathRegistrationWatcher;

public class CompassImportPathRegistrationWatcherRM extends CompassImportPathRegistrationWatcher {
  public CompassImportPathRegistrationWatcherRM(@NotNull final Module module) {
    super(module, new CompassConfigParserRM());
  }

  @Nullable
  public static CompassImportPathRegistrationWatcherRM getInstance(final Module module) {
    return ModuleServiceManager.getService(module, CompassImportPathRegistrationWatcherRM.class);
  }
}
