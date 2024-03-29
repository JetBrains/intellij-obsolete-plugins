package com.intellij.dmserver.libraries.obr;

import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;

public class JTableWrapper<T, C extends Column<T>> {

  private static final int COLUMN_HEADER_MARGIN_WIDTH = 2;

  private final C[] myColumns;

  private final JTableWrapperModel myTableModel;

  private final JBTable myTable;

  public JTableWrapper(JBTable table, C[] columns) {
    myTable = table;
    myColumns = columns;

    myTableModel = createTableModel();

    for (C column : myColumns) {
      myTableModel.addColumn(column.getName());
    }

    myTable.setModel(myTableModel);

    packColumns();

    myTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
  }

  private void packColumns() {
    DefaultTableColumnModel colModel = (DefaultTableColumnModel)myTable.getColumnModel();

    int[] colMinWidths = new int[myColumns.length];

    for (int iColumn = 0; iColumn < myColumns.length; iColumn++) {
      TableColumn col = colModel.getColumn(iColumn);

      colMinWidths[iColumn] = col.getMinWidth();

      if (!myColumns[iColumn].needPack()) {
        col.setWidth(col.getMinWidth());
        continue;
      }

      // Get width of column header
      TableCellRenderer renderer = col.getHeaderRenderer();
      if (renderer == null) {
        renderer = myTable.getTableHeader().getDefaultRenderer();
      }
      Component comp = renderer.getTableCellRendererComponent(myTable, col.getHeaderValue(), false, false, 0, 0);
      int colWidth = comp.getPreferredSize().width;

      for (int iRow = 0; iRow < myTable.getRowCount(); iRow++) {
        renderer = myTable.getCellRenderer(iRow, iColumn);
        comp = renderer.getTableCellRendererComponent(myTable, myTable.getValueAt(iRow, iColumn), false, false, iRow, iColumn);
        colWidth = Math.max(colWidth, comp.getPreferredSize().width);
      }

      colWidth += 2 * COLUMN_HEADER_MARGIN_WIDTH;

      col.setMaxWidth(colWidth);
      col.setMinWidth(colWidth);
    }

    for (int iColumn = 0; iColumn < myColumns.length; iColumn++) {
      TableColumn col = colModel.getColumn(iColumn);
      col.setMinWidth(colMinWidths[iColumn]);
    }
  }

  public C[] getColumns() {
    return myColumns;
  }

  private DefaultTableModel getTableModel() {
    return myTableModel;
  }

  public void setInput(Iterable<Collection<Object>> input) {
    getTableModel().setRowCount(0);
    for (Collection<Object> row : input) {
      getTableModel().addRow(row.toArray());
    }
    packColumns();
  }

  public void setInputRows(Iterable<? extends T> rows) {
    ArrayList<Collection<Object>> input = new ArrayList<>();
    for (T row : rows) {
      ArrayList<Object> rowCellValues = new ArrayList<>();
      for (C column : getColumns()) {
        rowCellValues.add(column.getColumnValue(row));
      }
      input.add(rowCellValues);
    }
    setInput(input);
  }

  public JBTable getTable() {
    return myTable;
  }

  protected JTableWrapperModel createTableModel() {
    return new JTableWrapperModel();
  }

  protected class JTableWrapperModel extends DefaultTableModel {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean isCellEditable(int row, int column) {
      return myColumns[column].isEditable();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      return myColumns[columnIndex].getValueClass();
    }

    @Override
    public void setValueAt(Object aValue, int row, int column) {
      myColumns[column].setColumnValue(row, aValue);
      super.setValueAt(aValue, row, column);
    }
  }
}
