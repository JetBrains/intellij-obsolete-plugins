/*
 * Copyright (c) 2005 JetBrains s.r.o. All Rights Reserved.
 */
package com.intellij.designer.inspector.impl.actions;

import com.intellij.designer.inspector.AbstractInspectorAction;
import com.intellij.designer.inspector.Property;
import com.intellij.designer.inspector.PropertyInspector;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author spleaner
 */
public class ExpandAllAction extends AbstractInspectorAction {
  private final PropertyInspector myInspector;

  protected ExpandAllAction(PropertyInspector inspector, String text, Icon icon) {
    super(text, icon);

    myInspector = inspector;
  }

  public ExpandAllAction(PropertyInspector inspector) {
    this(inspector, "Expand All", AllIcons.Actions.Expandall);
  }

  @Override
  protected PropertyInspector getInspector(@NotNull AnActionEvent e) {
    if (myInspector == null) {
      return super.getInspector(e);
    }

    return myInspector;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final PropertyInspector inspector = getInspector(e);
    final Property property = inspector.getSelectedProperty();

    perform(inspector);

    if (property != null) {
      inspector.setSelectedProperty(property);
    }
  }

  @Override
  protected boolean isEnabled(@NotNull final DataContext dataContext) {
    return true;
  }

  protected PropertyInspector getInspector() {
    return myInspector;
  }

  @Override
  protected void doUpdate(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabled(!myInspector.isEmpty());
  }

  protected void perform(final PropertyInspector inspector) {
    myInspector.expandAll();
  }

}
