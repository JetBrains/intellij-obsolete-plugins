// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.utils;

import com.intellij.DynamicBundle;
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public final class HelidonBundle {
  private static final @NonNls String BUNDLE = "messages.HelidonBundle";
  private static final DynamicBundle INSTANCE = new DynamicBundle(HelidonBundle.class, BUNDLE);

  public static final @NlsSafe String HELIDON_LIBRARY = "Helidon";

  private HelidonBundle() {
  }

  public static @Nls String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object @NotNull ... params) {
    return INSTANCE.getMessage(key, params);
  }
}
