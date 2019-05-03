/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.designer.inspector;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author spleaner
 */
public interface PropertyEditor<P extends Property> extends PropertyValidator<P> {

  @Nullable
  JComponent getEditorComponent(@NotNull final P property, final RenderingContext context);

  @Nullable
  JComponent getFocusableComponent();

  @Nullable
  Object getEditingValue();

  boolean stopEditing(boolean cancelled);

  boolean canEdit(P property);
}
