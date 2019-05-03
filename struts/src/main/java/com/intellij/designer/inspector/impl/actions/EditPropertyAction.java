package com.intellij.designer.inspector.impl.actions;

import com.intellij.designer.inspector.AbstractInspectorAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

/**
 * @author spleaner
 */
public class EditPropertyAction extends AbstractInspectorAction {

  public EditPropertyAction() {
    super("Edit property value");
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    getInspector(e).processEnter();
  }
}
