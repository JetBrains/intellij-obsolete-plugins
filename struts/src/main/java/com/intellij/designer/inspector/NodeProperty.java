package com.intellij.designer.inspector;

import com.intellij.openapi.actionSystem.AnActionEvent;

/**
 * @author spleaner
 */
public interface NodeProperty<P extends Property> {

  void createAndAddChildProperty(Property current, AnActionEvent e);

  void removeProperty(P property, AnActionEvent e);

  boolean isRemovable(Property property);
}
