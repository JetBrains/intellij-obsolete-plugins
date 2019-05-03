/*
 * Copyright (c) 2000-2006 JetBrains s.r.o. All Rights Reserved.
 */

package com.intellij.designer.inspector;

import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * @author spleaner
 */
public interface PresentationManager {
  Color getBackgroundColor(@NotNull final Property property, final boolean selected);
}
