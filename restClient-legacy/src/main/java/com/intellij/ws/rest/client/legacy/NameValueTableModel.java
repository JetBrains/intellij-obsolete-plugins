package com.intellij.ws.rest.client.legacy;

import com.intellij.util.ui.EditableModel;
import com.intellij.util.ui.ItemRemovable;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class NameValueTableModel extends AbstractTableModel implements ItemRemovable, EditableModel {
  private final List<String> names = new ArrayList<>();
  private final List<String> values = new ArrayList<>();
  private static final String EMPTY = "";
  private static final @NonNls String NAME = "Name";
  private static final @NonNls String VALUE = "Value";
  private final List<String> immutableHeaders = new ArrayList<>();
  private final List<String> immutableHeadersExceptions = new ArrayList<>();

  @Override
  public int getRowCount() {
    return names.size();
  }

  @Override
  public int getColumnCount() {
    return 2;
  }

  @Override
  public Object getValueAt(final int rowIndex, final int columnIndex) {
    List<String> list = (columnIndex == 0) ? names : values;
    return list.get(rowIndex);
  }

  @Override
  @NonNls
  public String getColumnName(final int column) {
    return column == 0 ? NAME : VALUE;
  }

  @Override
  public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
    List<String> list = (columnIndex == 0) ? names : values;
    final String value = String.valueOf(aValue);
    if (rowIndex < list.size()) {
      list.set(rowIndex, value);
      if (names.get(rowIndex).trim().equals(EMPTY) && rowIndex != 0) {
        names.remove(rowIndex);
        values.remove(rowIndex);
      }
    } else {
      if (aValue == null || value.trim().equals(EMPTY) ) return;
      addPropertyRow(EMPTY, EMPTY);
      list.set(rowIndex, value);
    }
    fireTableRowsUpdated(rowIndex, rowIndex);
  }

  public void addProperty(@NotNull @NonNls String name, @NotNull @NonNls String value) {
    if (names.contains(name)) {
      int index = getIndexByName(name);
      values.set(index, value);
      fireTableRowsUpdated(index, index);

    } else {
      addPropertyRow(name, value);
    }
  }

  public void addPropertyRow(String name, String value) {
    names.add(name);
    values.add(value);
    fireTableRowsInserted(names.size()-1, names.size()-1);
  }

  public void clear() {
    names.clear();
    values.clear();
    fireTableDataChanged();
  }

  @Override
  public boolean isCellEditable(final int row, final int column) {
    //if (row == 0 && column == 1) return true; //we can send files now
    if (column == 0 && row != 0
        && (names.isEmpty() || names.get(0) == null || names.get(0).trim().length() == 0)) {
      return false;
    }
    if (row >= names.size()) return column == 0;
    if (column == 0) {
      return !(immutableHeaders.contains(names.get(row)));
    } else {
        if (immutableHeadersExceptions.contains(names.get(row))) return true;
        if (immutableHeaders.contains(names.get(row))) return false;
    }
    return !names.get(row).trim().equals(EMPTY);
  }

  @NotNull
  public String getName(int index) {
    return index < names.size() ? names.get(index) : EMPTY;
  }

  @NotNull
  public String getValue(int index) {
    return index < values.size() ? values.get(index) : EMPTY;
  }

  public int getElementsCount() {
    return names.size();
  }

  public void setImmutableFields(List<String> headerNames) {
    immutableHeaders.clear();
    immutableHeaders.addAll(headerNames);
  }

  public void setImmutableFields(String... fields) {
    setImmutableFields(Arrays.asList(fields));
  }

  public void setImmutableHeaderExceptions(List<String> headerNames) {
    immutableHeadersExceptions.clear();
    immutableHeadersExceptions.addAll(headerNames);
  }

  public void setImmutableHeaderExceptions(String... fields) {
    setImmutableHeaderExceptions(Arrays.asList(fields));
  }

  public int getIndexByName(String name) {
    for (int i=0; i < names.size(); i++) {
      if (names.get(i).equals(name)) return i;
    }
    return -1;
  }

  @Override
  public void addRow() {
    names.add("");
    values.add("");
    int i = getElementsCount() - 1;
    fireTableRowsInserted(i, i);
  }

  @Override
  public void exchangeRows(int oldIndex, int newIndex) {
    Collections.swap(names, oldIndex, newIndex);
    Collections.swap(values, oldIndex, newIndex);
    fireTableRowsUpdated(Math.min(oldIndex, newIndex), Math.max(oldIndex, newIndex));
  }

  @Override
  public boolean canExchangeRows(int oldIndex, int newIndex) {
    return true;
  }

  @Override
  public void removeRow(int idx) {
    if (idx >= names.size()) return; // empty row was automatically removed in setValue call
    names.remove(idx);
    values.remove(idx);
    fireTableRowsDeleted(idx, idx);
  }
}