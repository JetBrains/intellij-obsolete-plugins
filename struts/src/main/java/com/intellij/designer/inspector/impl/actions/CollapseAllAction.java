/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.designer.inspector.impl.actions;

import com.intellij.designer.inspector.PropertyInspector;
import com.intellij.icons.AllIcons;

/**
 * @author spleaner
 */
public class CollapseAllAction extends ExpandAllAction {

  public CollapseAllAction(PropertyInspector inspector) {
    super(inspector, "Collapse All", AllIcons.Actions.Collapseall);
  }

  @Override
  protected void perform(final PropertyInspector inspector) {
    getInspector().collapseAll();
  }
}
