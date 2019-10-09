/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.execution;

import com.intellij.AbstractBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public class AppletBundle extends AbstractBundle {

  public static String message(@NotNull @PropertyKey(resourceBundle = PATH_TO_BUNDLE) String key, @NotNull Object... params) {
    return ourInstance.getMessage(key, params);
  }

  public static final String PATH_TO_BUNDLE = "com.intellij.execution.AppletBundle";
  private static final AbstractBundle ourInstance = new AppletBundle();

  private AppletBundle() {
    super(PATH_TO_BUNDLE);
  }
}
