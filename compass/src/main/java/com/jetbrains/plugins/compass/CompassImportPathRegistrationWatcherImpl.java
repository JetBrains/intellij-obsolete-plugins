package com.jetbrains.plugins.compass;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleServiceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompassImportPathRegistrationWatcherImpl extends CompassImportPathRegistrationWatcher {
  public CompassImportPathRegistrationWatcherImpl(@NotNull Module module) {
    super(module, new CompassConfigParserImpl());
  }

  @Nullable
  public static CompassImportPathRegistrationWatcherImpl getInstance(final Module module) {
    return ModuleServiceManager.getService(module, CompassImportPathRegistrationWatcherImpl.class);
  }
}
