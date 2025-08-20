package com.intellij.jboss.bpmn.jbpm.render.background;

import com.intellij.jboss.bpmn.jbpm.render.ChartNodeRenderer;
import com.intellij.jboss.bpmn.jbpm.render.RenderArgs;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class ChartNodeBorderRenderer<T> implements ChartNodeRenderer<T, RenderBorder> {
  @Override
  public void renderComponent(RenderBorder renderBorder, RenderArgs<T> renderArgs) {
    Border border = renderBorder.width() == 0 ? JBUI.Borders.empty() : createBorder(renderBorder);
    renderArgs.wrapper.setBorder(border);
    Container outerPanel = renderArgs.wrapper.getParent();
    if (outerPanel instanceof JPanel) {
      ((JPanel)outerPanel).setBorder(border);
    }
  }

  @Override
  public Class<RenderBorder> getLayoutClass() {
    return RenderBorder.class;
  }

  private static Border createBorder(RenderBorder border) {
    int[] rgba_norm = border.rgba();
    int[] rgba_dark = border.rgbaDark();
    return JBUI.Borders.customLine(
      new JBColor(
        new Color(rgba_norm[0], rgba_norm[1], rgba_norm[2], rgba_norm[3]),
        new Color(rgba_dark[0], rgba_dark[1], rgba_dark[2], rgba_dark[3])),
      border.width());
  }
}
