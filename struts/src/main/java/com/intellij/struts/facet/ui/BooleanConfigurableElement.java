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
