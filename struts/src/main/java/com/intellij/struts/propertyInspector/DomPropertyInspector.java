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

import com.intellij.designer.inspector.AbstractProperty;
import com.intellij.designer.inspector.DefaultPropertyNameRenderer;
import com.intellij.designer.inspector.PropertyInspector;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Factory;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomManager;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.reflect.DomAttributeChildDescription;
import com.intellij.util.xml.reflect.DomFixedChildDescription;
import com.intellij.xml.XmlElementDescriptor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class DomPropertyInspector extends PropertyInspector implements Disposable {

  public DomPropertyInspector() {

    setNameRenderer(DomProperty.class, new DefaultPropertyNameRenderer<>());
    final DomPropertyEditor domPropertyEditor = new DomPropertyEditor();
    Disposer.register(this, domPropertyEditor);
    setValueEditor(DomProperty.class, domPropertyEditor);
    setValueRenderer(DomProperty.class, new DomPropertyRenderer());

    setElement(null);
  }

  public void setElement(@Nullable final DomElement element) {

    AbstractProperty root = AbstractProperty.root();
    if (element != null) {
      final DomManager domManager = element.getManager();
      final XmlTag tag = element.getXmlTag();
      final XmlElementDescriptor descriptor = tag == null ? null : tag.getDescriptor();
      for (final DomAttributeChildDescription description : element.getGenericInfo().getAttributeChildrenDescriptions()) {
        GenericAttributeValue value = domManager.createStableValue(
          () -> element.isValid() ? description.getDomAttributeValue(element) : null);
        if (descriptor == null || descriptor.getAttributeDescriptor(value.getXmlElementName(), tag) != null) {
          DomProperty property = new DomProperty(value);
          root.add(property);
        }
      }
      for (final DomFixedChildDescription description : element.getGenericInfo().getFixedChildrenDescriptions()) {
        if (description.getCount() == 1) {
          final DomElement domElement = domManager.createStableValue((Factory<DomElement>)() -> {
            if (!element.isValid()) {
              return null;
            }
            final List<? extends DomElement> values = description.getValues(element);
            return values.get(0);
          });
          if (domElement instanceof GenericDomValue) {
            DomProperty property = new DomProperty((GenericDomValue)domElement);
            root.add(property);
          }
        }
      }
    }
    setRoot(root);
  }

  @Override
  public void dispose() {

  }
}
