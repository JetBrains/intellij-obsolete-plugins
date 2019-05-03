/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.propertyInspector;

import com.intellij.designer.inspector.PropertyEditor;
import com.intellij.designer.inspector.RenderingContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.ui.DomUIControl;
import com.intellij.util.xml.ui.DomUIFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * @author Dmitry Avdeev
 */
public class DomPropertyEditor implements PropertyEditor<DomProperty>, Disposable {

  private DomUIControl<GenericDomValue> myDomControl;
  private DomProperty myProperty;

  @Override
  public JComponent getEditorComponent(@NotNull final DomProperty property, final RenderingContext context) {
    myProperty = property;
    myDomControl = DomUIFactory.createControl(property.getDomValue());
    myDomControl.reset();
    Disposer.register(this, myDomControl);
    final JComponent component = myDomControl.getComponent();
    component.setBorder(new EmptyBorder(0, 1, 0, 0));
    return component;
  }

  @Override
  public JComponent getFocusableComponent() {
    return myDomControl.getComponent();
  }

  @Override
  public Object getEditingValue() {
    return myDomControl.getDomElement().getValue();
  }

  @Override
  public boolean stopEditing(final boolean cancelled) {
    if (cancelled) {
      myDomControl.reset();
    }
    else {
      myDomControl.commit();
      myProperty.refresh();
    }
    return true;
  }

  @Override
  public boolean canEdit(final DomProperty property) {
    return property.getDomValue().isValid();
  }

  @Override
  public boolean accepts(final DomProperty property) {
    return false;
  }

  @Override
  public void dispose() {
  }
}
