package com.intellij.jboss.bpmn.jbpm.providers;

import com.intellij.diagram.DiagramNode;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public abstract class ColorProviderImpl implements ColorProvider {
  @NotNull final private Color color;

  protected ColorProviderImpl(@NotNull Color colorRegular, @NotNull Color colorDark) {
    this.color = new JBColor(colorRegular, colorDark);
  }

  protected ColorProviderImpl(@NotNull Color color) {
    this.color = new JBColor(color, color);
  }

  @NotNull
  @Override
  public Color getColor(DiagramNode node) {
    return color;
  }
}
