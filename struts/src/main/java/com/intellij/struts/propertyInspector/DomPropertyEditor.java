/*
 * Copyright 2000-2006 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
