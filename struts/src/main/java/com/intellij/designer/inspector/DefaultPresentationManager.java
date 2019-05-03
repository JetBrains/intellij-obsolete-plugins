/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.designer.inspector;

import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * @author spleaner
 */
public class DefaultPresentationManager<Inspector extends PropertyInspector> implements PresentationManager {

  private final Inspector myInspector;

  public DefaultPresentationManager(@NotNull final Inspector inspector) {
    myInspector = inspector;
  }

  public Inspector getInspector() {
    return myInspector;
  }

  @Override
  public Color getBackgroundColor(@NotNull final Property property, final boolean selected) {
    if (selected) {
      return myInspector.getSelectionBackground();
    }

    return myInspector.getBackground();
  }
}
