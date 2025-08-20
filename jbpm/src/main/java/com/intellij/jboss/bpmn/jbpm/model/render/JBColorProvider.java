package com.intellij.jboss.bpmn.jbpm.model.render;

import com.intellij.diagram.DiagramNode;
import com.intellij.jboss.bpmn.jbpm.providers.ColorProviderImpl;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class JBColorProvider extends ColorProviderImpl {
  public JBColorProvider() {
    //noinspection UseJBColor
    super(Gray._1);
  }

  @NotNull
  @Override
  public Color getColor(DiagramNode node) {
    return JBColor.foreground();
  }
}
