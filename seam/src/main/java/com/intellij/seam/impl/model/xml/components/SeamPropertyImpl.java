package com.intellij.seam.impl.model.xml.components;

import com.intellij.psi.PsiType;
import com.intellij.psi.impl.beanProperties.BeanProperty;
import com.intellij.seam.model.xml.components.SeamProperty;

public abstract class SeamPropertyImpl implements SeamProperty {
  @Override
  public String getPropertyName() {
    return getName().getStringValue();
  }

  @Override
  public PsiType getPropertyType() {
    final BeanProperty beanProperty = getName().getValue();
    if (beanProperty == null) return null;

    return beanProperty.getPropertyType();
  }
}
