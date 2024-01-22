package com.intellij.jboss.bpmn.jbpm.providers;

import com.intellij.diagram.DiagramNode;
import com.intellij.jboss.bpmn.jbpm.render.pictures.RenderImage;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class DefaultImageProvider<T, Node extends DiagramNode<T>> implements ImageProvider<T, Node> {
  @Override
  public @NotNull Icon getImage(Node node, RenderImage renderImage) {
    return IconLoader.getIcon(renderImage.icon(), DefaultImageProvider.class.getClassLoader());
  }
}
