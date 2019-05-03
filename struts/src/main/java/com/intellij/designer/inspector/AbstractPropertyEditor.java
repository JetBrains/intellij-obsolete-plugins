/*
 * Copyright (c) 2000-2006 JetBrains s.r.o. All Rights Reserved.
 */

package com.intellij.designer.inspector;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author spleaner
 */
public abstract class AbstractPropertyEditor<P extends Property, C extends JComponent> implements PropertyEditor<P> {
  private final C myComponent;
  private P myProperty;


  protected AbstractPropertyEditor(@NotNull final C c) {
    myComponent = c;
  }

  protected C getComponent() {
    return myComponent;
  }

  @Override
  public JComponent getEditorComponent(final P property, final RenderingContext context) {
    myProperty = property;
    return myComponent;
  }

  @Override
  public JComponent getFocusableComponent() {
    return myComponent;
  }

  @Override
  public P getEditingValue() {
    return myProperty;
  }

  @Override
  public boolean stopEditing(final boolean cancelled) {
    return true;
  }

  @Override
  public boolean canEdit(final P property) {
    return false;
  }

  @Override
  public boolean accepts(final P property) {
    return false;
  }
}
