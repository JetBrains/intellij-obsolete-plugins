/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.facet.ui;

import com.intellij.openapi.util.Ref;

import javax.swing.*;

/**
 * @author Dmitry Avdeev
 */
public  class BooleanConfigurableElement implements ConfigurableElement {
  private final JCheckBox myCheckBox;
  private final Ref<Boolean> myField;

  public BooleanConfigurableElement(final JCheckBox checkBox, final Ref<Boolean> field) {
    myField = field;
    myCheckBox = checkBox;
  }

  @Override
  public boolean isModified() {
    return myCheckBox.isSelected() != myField.get().booleanValue();
  }

  /**
   * Store the settings from configurable to other components.
   */
  @Override
  public void apply() {
    myField.set(myCheckBox.isSelected());
  }

  /**
   * Load settings from other components to configurable.
   */
  @Override
  public void reset() {
    myCheckBox.setSelected(myField.get().booleanValue());
  }


  public JCheckBox getCheckBox() {
    return myCheckBox;
  }
}
