package com.intellij.designer.inspector;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author spleaner
 */
public abstract class AbstractInspectorAction extends AnAction {
  public AbstractInspectorAction(String text) {
    super(text);
  }

  protected AbstractInspectorAction(String text, Icon icon) {
    super(text, "", icon);
  }

  @Nullable
  protected Property getSelectedProperty(@NotNull AnActionEvent e) {
    final PropertyInspector inspector = getInspector(e);
    return inspector == null ? null : inspector.getSelectedProperty();
  }

  @Override
  public final void update(@NotNull AnActionEvent e) {
    if (isEnabled(e.getDataContext())) {
      doUpdate(e);
    }
    else {
      e.getPresentation().setEnabled(false);
    }
  }

  protected void doUpdate(@NotNull AnActionEvent e) {
  }

  protected boolean isEnabled(@NotNull DataContext dataContext) {
    return dataContext.getData(PropertyInspector.INSPECTOR_KEY) != null;
  }

  @Nullable
  protected PropertyInspector getInspector(@NotNull AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    return (PropertyInspector)dataContext.getData(PropertyInspector.INSPECTOR_KEY);
  }

  @Nullable
  protected static NodeProperty getNodeProperty(Property property) {
    Property result = (property instanceof NodeProperty) ? property : null;
    if (property != null) {
      Property parent = property.getParentProperty();
      while (result == null && parent != null) {
        result = parent instanceof NodeProperty ? parent : null;
        parent = parent.getParentProperty();
      }
    }

    return (NodeProperty) result;
  }
}
