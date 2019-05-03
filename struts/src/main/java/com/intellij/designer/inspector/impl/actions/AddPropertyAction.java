package com.intellij.designer.inspector.impl.actions;

import com.intellij.designer.inspector.AbstractInspectorAction;
import com.intellij.designer.inspector.NodeProperty;
import com.intellij.designer.inspector.Property;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

/**
 * @author spleaner
 */
public class AddPropertyAction extends AbstractInspectorAction {

  public AddPropertyAction() {
    super("Add Property");
  }

  protected AddPropertyAction(String text) {
    super(text);
  }

  @Override
  public void doUpdate(@NotNull AnActionEvent e) {
    final Property property = getSelectedProperty(e);
    e.getPresentation().setEnabled(getNodeProperty(property) != null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Property property = getSelectedProperty(e);
    assert property != null;

    final NodeProperty nodeProperty = getNodeProperty(property);
    assert nodeProperty != null;
    nodeProperty.createAndAddChildProperty(property, e);
  }
}
