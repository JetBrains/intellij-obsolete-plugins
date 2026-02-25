// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.mvc.plugins;

import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.ColumnInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.FontMetrics;

public class MvcPluginsTable extends JBTable {
  public MvcPluginsTable(final AvailablePluginsModel model) {
    super(model);

    initializeHeader();

    for (int i = 0; i < model.getColumnCount(); i++) {
      TableColumn column = getColumnModel().getColumn(i);
      final ColumnInfo columnInfo = model.getColumnInfos()[i];
      column.setCellEditor(columnInfo.getEditor(null));
      if (i == 0 || i == 2) {
        String name = columnInfo.getName();
        final FontMetrics fontMetrics = getFontMetrics(getFont());
        int width = fontMetrics.stringWidth(" " + name + " ");

        if (i == 2) {
          width += fontMetrics.stringWidth(name);
        }

        column.setWidth(width);
        column.setPreferredWidth(width);
        column.setMaxWidth(width);
      }
    }

    setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    setShowGrid(false);
  }

  @Override
  public void setValueAt(final Object aValue, final int row, final int column) {
    super.setValueAt(aValue, row, column);
    repaint(); //in order to update invalid plugins
  }

  @Override
  public @Nullable TableCellRenderer getCellRenderer(final int row, final int column) {
    final ColumnInfo columnInfo = getModel().getColumnInfos()[column];
    return columnInfo.getRenderer(null);
  }

  private void initializeHeader() {
    final JTableHeader header = getTableHeader();
    header.setReorderingAllowed(false);
  }

  public Object[] getElements() {
    return getModel().getAvailablePlugins().toArray();
  }

  public @NotNull MvcPluginDescriptor getPluginAt(int row) {
    return getModel().getRowValue(convertRowIndexToModel(row));
  }

  @Override
  public AvailablePluginsModel getModel() {
    return (AvailablePluginsModel)super.getModel();
  }

  public @Nullable MvcPluginDescriptor getSelectedObject() {
    if (getSelectedRowCount() > 0) {
      return getPluginAt(getSelectedRow());
    }

    return null;
  }

  @Override
  protected boolean isSortOnUpdates() {
    return false;
  }

  public MvcPluginDescriptor @NotNull [] getSelectedObjects() {
    if (getSelectedRowCount() == 0) return MvcPluginDescriptor.EMPTY_ARRAY;

    int[] poses = getSelectedRows();
    MvcPluginDescriptor[] selection = new MvcPluginDescriptor[poses.length];
    for (int i = 0; i < poses.length; i++) {
      selection[i] = getPluginAt(poses[i]);
    }

    return selection;
  }
}