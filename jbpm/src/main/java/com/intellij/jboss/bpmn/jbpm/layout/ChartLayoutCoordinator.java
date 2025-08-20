package com.intellij.jboss.bpmn.jbpm.layout;

import com.intellij.jboss.bpmn.jbpm.render.size.ChartNodeSizeEnhancer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ChartLayoutCoordinator {
  @Nullable
  NodeLayout getNodeLayout(@NotNull String nodeId, ChartNodeSizeEnhancer enhancer);

  @Nullable
  Runnable getChangeNodeLayoutAction(@NotNull String nodeId,
                                     @NotNull NodeLayout layout,
                                     @Nullable ChartNodeSizeEnhancer enhancer);

  @Nullable
  List<Point> getEdgePoints(@NotNull String sourceNodeId,
                            @NotNull String targetNodeId);

  @Nullable
  Runnable getChangeEdgePointsAction(@NotNull String sourceNodeId,
                                     @NotNull String targetNodeId,
                                     @NotNull List<Point> points);

  class Point {
    public final double x;
    public final double y;

    public Point(double x, double y) {
      this.x = x;
      this.y = y;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Point point = (Point)o;

      if (Double.compare(point.x, x) != 0) return false;
      if (Double.compare(point.y, y) != 0) return false;

      return true;
    }

    @Override
    public int hashCode() {
      return 31 * Double.hashCode(x) + Double.hashCode(y);
    }
  }

  class Size {
    public final double width;
    public final double height;

    public Size(double width, double height) {
      this.width = width;
      this.height = height;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Size size = (Size)o;

      if (Double.compare(size.width, width) != 0) return false;
      if (Double.compare(size.height, height) != 0) return false;

      return true;
    }

    @Override
    public int hashCode() {
      return 31 * Double.hashCode(width) + Double.hashCode(height);
    }
  }

  final class NodeLayout {
    @NotNull private final Point center;
    @NotNull private final Size size;

    private NodeLayout(double centerX, double centerY, double width, double height) {
      this.center = new Point(centerX, centerY);
      this.size = new Size(width, height);
    }

    private NodeLayout(@NotNull Size size, @NotNull Point center) {
      this.size = size;
      this.center = center;
    }

    @Override
    public String toString() {
      return "NodeLayout(" + center.x + "," + center.y + ")[" + size.width + "," + size.height + "]";
    }

    public double getLeft() {
      return center.x - size.width / 2;
    }

    public double getRight() {
      return center.x + size.width / 2;
    }

    public double getTop() {
      return center.y - size.height / 2;
    }

    public double getBottom() {
      return center.y + size.height / 2;
    }

    public double getCenterX() {
      return center.x;
    }

    public double getCenterY() {
      return center.y;
    }

    public double getWidth() {
      return size.width;
    }

    public double getHeight() {
      return size.height;
    }

    @NotNull
    public Point getCenter() {
      return center;
    }

    @NotNull
    public Size getSize() {
      return size;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      NodeLayout layout = (NodeLayout)o;

      if (!center.equals(layout.center)) return false;
      if (!size.equals(layout.size)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = center.hashCode();
      result = 31 * result + size.hashCode();
      return result;
    }

    static public NodeLayout createByCenterPoint(@NotNull Size size, @NotNull Point center) {
      return new NodeLayout(size, center);
    }

    static public NodeLayout createByCenterPoint(double centerX, double centerY, double width, double height) {
      return new NodeLayout(centerX, centerY, width, height);
    }

    static public NodeLayout createByEdges(double left, double top, double right, double bottom) {
      return new NodeLayout(
        (left + right) / 2,
        (top + bottom) / 2,
        Math.abs(right - left),
        Math.abs(bottom - top));
    }
  }
}
