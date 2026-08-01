package org.jetbrains.jps.gwt.build;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.builders.BuildTargetLoader;
import org.jetbrains.jps.builders.BuildTargetType;
import org.jetbrains.jps.gwt.model.JpsGwtExtensionService;
import org.jetbrains.jps.gwt.model.JpsGwtModuleExtension;
import org.jetbrains.jps.model.JpsModel;
import org.jetbrains.jps.model.module.JpsModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GwtBuildTargetType extends BuildTargetType<GwtBuildTarget> {
  public static final GwtBuildTargetType INSTANCE = new GwtBuildTargetType();
  public static final String TYPE_ID = "gwt";

  private GwtBuildTargetType() {
    super(TYPE_ID, true);
  }

  @Override
  public @NotNull List<GwtBuildTarget> computeAllTargets(@NotNull JpsModel model) {
    List<GwtBuildTarget> targets = new ArrayList<>();
    JpsGwtExtensionService service = JpsGwtExtensionService.getInstance();
    for (JpsModule module : model.getProject().getModules()) {
      JpsGwtModuleExtension extension = service.getExtension(module);
      if (extension != null) {
        targets.add(new GwtBuildTarget(extension));
      }
    }
    return targets;
  }

  @Override
  public @NotNull BuildTargetLoader<GwtBuildTarget> createLoader(@NotNull JpsModel model) {
    return new Loader(model);
  }

  private static class Loader extends BuildTargetLoader<GwtBuildTarget> {
    private final Map<String, GwtBuildTarget> myTargets;

    Loader(JpsModel model) {
      myTargets = new HashMap<>();
      JpsGwtExtensionService service = JpsGwtExtensionService.getInstance();
      for (JpsModule module : model.getProject().getModules()) {
        JpsGwtModuleExtension extension = service.getExtension(module);
        if (extension != null) {
          myTargets.put(module.getName(), new GwtBuildTarget(extension));
        }
      }
    }

    @Override
    public @Nullable GwtBuildTarget createTarget(@NotNull String targetId) {
      return myTargets.get(targetId);
    }
  }
}
