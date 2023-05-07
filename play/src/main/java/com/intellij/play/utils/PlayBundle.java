package com.intellij.play.utils;

import com.intellij.DynamicBundle;
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

public final class PlayBundle {
  private static final @NonNls String BUNDLE = "messages.PlayBundle";
  private static final DynamicBundle INSTANCE = new DynamicBundle(PlayBundle.class, BUNDLE);

  public static final @NlsSafe String PLAY_FRAMEWORK = "Play Framework";

  private PlayBundle() {}

  public static @NotNull @Nls String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object @NotNull ... params) {
    return INSTANCE.getMessage(key, params);
  }

  public static @NotNull Supplier<@Nls String> messagePointer(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object @NotNull ... params) {
    return INSTANCE.getLazyMessage(key, params);
  }
}

