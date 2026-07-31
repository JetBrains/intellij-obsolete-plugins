package org.jetbrains.jps.gwt.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.builders.storage.BuildDataPaths;
import org.jetbrains.jps.gwt.index.JpsGwtModuleIndex;
import org.jetbrains.jps.model.JpsModel;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.service.JpsServiceManager;

public abstract class JpsGwtExtensionService {
  public static JpsGwtExtensionService getInstance() {
    return JpsServiceManager.getInstance().getService(JpsGwtExtensionService.class);
  }

  public abstract @Nullable JpsGwtModuleExtension getExtension(@Nullable JpsModule module);

  public abstract void setExtension(@NotNull JpsModule module, @NotNull JpsGwtModuleExtension extension);

  public abstract @NotNull JpsGwtModuleIndex getGwtModuleIndex(@NotNull JpsModel model, @NotNull BuildDataPaths paths);
}
