/*
 * Copyright (c) 2000-2006 JetBrains s.r.o. All Rights Reserved.
 */

package com.intellij.designer.inspector;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author spleaner
 */
public interface PropertyRenderer<P extends Property> extends PropertyValidator<P> {
  JComponent getRendererComponent(@NotNull final P property, @NotNull final RenderingContext context);
}
