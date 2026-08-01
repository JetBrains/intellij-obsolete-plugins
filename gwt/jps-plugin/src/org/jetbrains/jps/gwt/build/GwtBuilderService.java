package org.jetbrains.jps.gwt.build;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.builders.BuildTargetType;
import org.jetbrains.jps.incremental.BuilderService;
import org.jetbrains.jps.incremental.TargetBuilder;

import java.util.Collections;
import java.util.List;

public final class GwtBuilderService extends BuilderService {
  @Override
  public @NotNull List<? extends BuildTargetType<?>> getTargetTypes() {
    return Collections.singletonList(GwtBuildTargetType.INSTANCE);
  }

  @Override
  public @NotNull List<? extends TargetBuilder<?,?>> createBuilders() {
    return Collections.singletonList(new GwtBuilder());
  }
}
