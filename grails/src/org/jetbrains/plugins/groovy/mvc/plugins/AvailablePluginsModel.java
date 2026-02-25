// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.mvc.plugins;

import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.SortableColumnModel;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.GrailsBundle;

import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class AvailablePluginsModel extends AbstractTableModel implements SortableColumnModel {
  public static final int COLUMN_IS_INSTALLED = 0;
  public static final int COLUMN_NAME = 1;
  public static final int COLUMN_VERSION = 2;
  public static final int COLUMN_TITLE = 3;

  protected ColumnInfo<MvcPluginDescriptor, Object>[] myColumns;

  private final List<MvcPluginDescriptor> myAvailablePlugins = new ArrayList<>();

  public AvailablePluginsModel(final Set<String> installedPlugins,
                               final @Nullable Collection<? extends MvcPluginDescriptor> availablePlugins) {
    super();

    //noinspection unchecked
    myColumns = new ColumnInfo[]{
      new MvcPluginIsInstalledColumnInfo(installedPlugins),

      new MvcPluginColumnInfo(this, GrailsBundle.message("column.info.mvc.plugin.name"), 150) {
        @Override
        public String valueOf(MvcPluginDescriptor mvcPlugin) {
          return mvcPlugin.getName();
        }
      },

      new MvcPluginColumnInfo(this, GrailsBundle.message("column.info.mvc.plugin.version"), 20) {
        @Override
        public String valueOf(MvcPluginDescriptor mvcPlugin) {
          return mvcPlugin.getLatestVersion();
        }
      },

      new MvcPluginColumnInfo(this, GrailsBundle.message("column.info.mvc.plugin.title"), 150) {
        @Override
        public String valueOf(MvcPluginDescriptor mvcPlugin) {
          return mvcPlugin.getTitle();
        }
      }};

    this.myAvailablePlugins.addAll(availablePlugins);

    fireModelChange();
  }

  @Override
  public RowSorter.SortKey getDefaultSortKey() {
    return new RowSorter.SortKey(COLUMN_IS_INSTALLED, SortOrder.ASCENDING);
  }

  @Override
  public MvcPluginDescriptor getRowValue(int row) {
    return myAvailablePlugins.get(row);
  }

  public void setData(final Collection<MvcPluginDescriptor> list) {
    myAvailablePlugins.clear();
    myAvailablePlugins.addAll(list);

    fireModelChange();
  }

  public List<MvcPluginDescriptor> getAvailablePlugins() {
    return myAvailablePlugins;
  }

  private void fireModelChange() {
    fireTableDataChanged();
  }

  @Override
  public int getColumnCount() {
    return myColumns.length;
  }

  @Override
  public ColumnInfo[] getColumnInfos() {
    return myColumns;
  }

  @Override
  public boolean isSortable() {
    return true;
  }

  @Override
  public void setSortable(boolean aBoolean) {
    // do nothing cause it's always sortable
  }

  @Override
  public String getColumnName(int column) {
    return myColumns[column].getName();
  }

  @Override
  public int getRowCount() {
    return myAvailablePlugins.size();
  }

  @Override
  public @Nullable Object getValueAt(int rowIndex, int columnIndex) {
    return myColumns[columnIndex].valueOf(myAvailablePlugins.get(rowIndex));
  }

  @Override
  public boolean isCellEditable(final int rowIndex, final int columnIndex) {
    return myColumns[columnIndex].isCellEditable(myAvailablePlugins.get(rowIndex));
  }

  @Override
  public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
    myColumns[columnIndex].setValue(myAvailablePlugins.get(rowIndex), aValue);
    fireTableRowsUpdated(rowIndex, rowIndex);
  }
}
