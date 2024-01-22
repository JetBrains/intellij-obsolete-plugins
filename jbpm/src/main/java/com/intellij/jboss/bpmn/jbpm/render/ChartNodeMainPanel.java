package com.intellij.jboss.bpmn.jbpm.render;

import com.intellij.ui.CellRendererPanel;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class ChartNodeMainPanel extends CellRendererPanel {
  @NotNull private final List<Painter> painters = new ArrayList<>();

  public ChartNodeMainPanel(LayoutManager layout) {
    setLayout(layout);
  }

  public void addPainter(Painter painters) {
    this.painters.add(painters);
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    for (Painter painter : painters) {
      painter.paintComponent(this, g);
    }
  }

  public static abstract class Painter {
    public abstract void paintComponent(ChartNodeMainPanel panel, Graphics graphics);
  }
}
