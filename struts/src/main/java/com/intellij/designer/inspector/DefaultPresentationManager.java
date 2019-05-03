/*
 * Copyright (c) 2000-2006 JetBrains s.r.o. All Rights Reserved.
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
