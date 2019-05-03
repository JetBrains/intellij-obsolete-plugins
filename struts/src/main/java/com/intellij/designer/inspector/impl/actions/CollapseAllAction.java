/*
 * Copyright (c) 2005 JetBrains s.r.o. All Rights Reserved.
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
