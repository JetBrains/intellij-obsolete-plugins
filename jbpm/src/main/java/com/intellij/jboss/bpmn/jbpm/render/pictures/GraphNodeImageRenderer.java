package com.intellij.jboss.bpmn.jbpm.render.pictures;

import com.intellij.jboss.bpmn.jbpm.providers.ImageProvider;
import com.intellij.jboss.bpmn.jbpm.providers.ProvidersCoordinator;
import com.intellij.jboss.bpmn.jbpm.render.ChartNodeMainPanel;
import com.intellij.jboss.bpmn.jbpm.render.ChartNodeRenderer;
import com.intellij.jboss.bpmn.jbpm.render.RenderArgs;
import com.intellij.openapi.util.ScalableIcon;

import javax.swing.*;
import java.awt.*;

public class GraphNodeImageRenderer<T> implements ChartNodeRenderer<T, RenderImage> {

  @Override
  public void renderComponent(RenderImage image, RenderArgs<T> renderArgs) {
    ImageProvider imageProvider = ProvidersCoordinator.getInstance().getProvider(image.imageProvider());
    @SuppressWarnings("unchecked") final Icon icon = imageProvider.getImage(renderArgs.chartNode, image);
    renderArgs.panel.addPainter(new ChartNodeMainPanel.Painter() {
      @Override
      public void paintComponent(ChartNodeMainPanel panel, Graphics graphics) {
        float scale = icon.getIconWidth() > 0 ? 1f * panel.getWidth() / icon.getIconWidth() : 1f;
        Icon scaled = ((ScalableIcon)icon).scale(scale);
        scaled.paintIcon(null, graphics, 0, 0);
      }
    });
  }

  @Override
  public Class<RenderImage> getLayoutClass() {
    return RenderImage.class;
  }
}
