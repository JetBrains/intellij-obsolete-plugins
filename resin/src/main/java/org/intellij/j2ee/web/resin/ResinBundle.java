/*
 * Copyright (c) 2005 JetBrains s.r.o. All Rights Reserved.
 */
package org.intellij.j2ee.web.resin;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

public final class ResinBundle {
  public static final @NonNls String BUNDLE = "messages.ResinBundle";
  private static final DynamicBundle INSTANCE = new DynamicBundle(ResinBundle.class, BUNDLE);

  private ResinBundle() {}

  public static @NotNull @Nls String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object @NotNull ... params) {
    return INSTANCE.getMessage(key, params);
  }

  public static @NotNull Supplier<@Nls String> messagePointer(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object @NotNull ... params) {
    return INSTANCE.getLazyMessage(key, params);
  }
}
