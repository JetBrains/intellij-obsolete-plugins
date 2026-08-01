package org.jetbrains.jps.gwt.build;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.builders.storage.BuildDataPaths;

import java.io.File;

public final class JpsGwtCompilerPaths {
  public static File getCompilerOutputRoot(@NotNull GwtBuildTarget target, final BuildDataPaths dataPaths) {
    return dataPaths.getTargetDataRootDir(target).resolve("gwt-output").toFile();
  }

  public static File getExtraOutputRoot(@NotNull GwtBuildTarget target, final BuildDataPaths dataPaths) {
    return dataPaths.getTargetDataRootDir(target).resolve("gwt-extra-output").toFile();
  }
}
