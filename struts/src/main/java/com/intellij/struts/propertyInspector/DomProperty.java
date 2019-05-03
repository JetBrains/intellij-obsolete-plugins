/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.propertyInspector;

import com.intellij.designer.inspector.AbstractProperty;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

/**
 * @author Dmitry Avdeev
 */
public class DomProperty extends AbstractProperty<String, String> implements Comparable<DomProperty> {

  private final GenericDomValue myDomValue;
  private Object myValueObject;

  public Class getObjectClass() {
    return myClass;
  }

  private final Class myClass;

  public DomProperty(final GenericDomValue value) {
    super(value.getXmlElementName(), value.getStringValue());
    Type type = value.getDomElementType();
    myClass = DomUtil.getGenericValueParameter(type);
    if (myClass == Boolean.class || myClass == boolean.class) {
      myValueObject = value.getValue();
    } else {
      myValueObject = value.getStringValue();
    }

    myDomValue = value;
  }

  @Override
  public void setValue(final String value) {
    myDomValue.setStringValue(value);
    refresh();
  }

  public GenericDomValue getDomValue() {
    return myDomValue;
  }

  void refresh() {
    final String stringValue = myDomValue.getStringValue();
    super.setValue(stringValue);
    if (myClass == Boolean.class || myClass == boolean.class) {
      myValueObject = myDomValue.getValue();
    } else {
      myValueObject = stringValue;
    }
  }

  @Override
  public int compareTo(final DomProperty o) {
    return getName().compareTo(o.getName());
  }

  @Override
  public boolean isValid() {
    return myDomValue.isValid();
  }

  @Nullable
  public Object getValueObject() {
    return myValueObject;
  }
}
