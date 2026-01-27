// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spring.osgi;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public final class SpringOsgiBundle extends DynamicBundle {

  public static @Nls String message(@NotNull @PropertyKey(resourceBundle = PATH_TO_BUNDLE) String key, Object @NotNull ... params) {
    return ourInstance.getMessage(key, params);
  }

  private static final @NonNls String PATH_TO_BUNDLE = "messages.SpringOsgiBundle";
  private static final SpringOsgiBundle ourInstance = new SpringOsgiBundle();

  private SpringOsgiBundle() {
    super(PATH_TO_BUNDLE);
  }
}

