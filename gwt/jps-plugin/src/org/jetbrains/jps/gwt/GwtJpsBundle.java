package org.jetbrains.jps.gwt;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;
import org.jetbrains.jps.api.JpsDynamicBundle;

public final class GwtJpsBundle {
  private static final @NonNls String BUNDLE = "messages.GwtJpsBundle";
  private static final JpsDynamicBundle INSTANCE = new JpsDynamicBundle(GwtJpsBundle.class, BUNDLE);

  private GwtJpsBundle() {
  }

  public static @NotNull @Nls String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object @NotNull ... params) {
    return INSTANCE.getMessage(key, params);
  }
}
