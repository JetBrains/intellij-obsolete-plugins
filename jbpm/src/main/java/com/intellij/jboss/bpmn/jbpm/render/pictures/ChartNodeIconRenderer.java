package com.intellij.jboss.bpmn.jbpm.render.pictures;

import com.intellij.jboss.bpmn.jbpm.providers.IconProvider;
import com.intellij.jboss.bpmn.jbpm.providers.ProvidersCoordinator;
import com.intellij.jboss.bpmn.jbpm.render.ChartNodeMainPanel;
import com.intellij.jboss.bpmn.jbpm.render.ChartNodeRenderer;
import com.intellij.jboss.bpmn.jbpm.render.RenderArgs;

import javax.swing.*;
import java.awt.*;

public class ChartNodeIconRenderer<T> implements ChartNodeRenderer<T, RenderIcon> {

  @Override
  public void renderComponent(final RenderIcon renderIcon, final RenderArgs<T> renderArgs) {
    IconProvider iconProvider = ProvidersCoordinator.getInstance().getProvider(renderIcon.iconProvider());
    @SuppressWarnings("unchecked") final Icon icon = iconProvider.getImage(renderArgs.chartNode, renderIcon);
    renderArgs.panel.addPainter(new ChartNodeMainPanel.Painter() {
      @Override
      public void paintComponent(ChartNodeMainPanel panel, Graphics graphics) {
        icon.paintIcon(
          panel,
          graphics,
          getPosition(panel.getWidth(), icon.getIconWidth(), renderIcon.horizontalAlignment().ordinal()),
          getPosition(panel.getHeight(), icon.getIconHeight(), renderIcon.verticalAlignment().ordinal()));
      }
    });
  }

  @Override
  public Class<RenderIcon> getLayoutClass() {
    return RenderIcon.class;
  }

  private static int getPosition(int frameSize, int iconSize, int alignmentOrdinal) {
    return (frameSize - iconSize) * alignmentOrdinal / 2;
  }
}
