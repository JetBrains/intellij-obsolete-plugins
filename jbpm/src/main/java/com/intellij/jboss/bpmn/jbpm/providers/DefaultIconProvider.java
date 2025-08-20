package com.intellij.jboss.bpmn.jbpm.providers;

import com.intellij.diagram.DiagramNode;
import com.intellij.jboss.bpmn.jbpm.render.pictures.RenderIcon;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class DefaultIconProvider<T, Node extends DiagramNode<T>> implements IconProvider<T, Node> {
  @NotNull
  @Override
  public Icon getImage(Node node, RenderIcon renderIcon) {
    return IconLoader.getIcon(renderIcon.icon(), DefaultIconProvider.class.getClassLoader());
  }
}
