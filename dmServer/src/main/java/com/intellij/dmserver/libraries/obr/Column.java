package com.intellij.dmserver.libraries.obr;

public abstract class Column<T> {

  private final String myName;

  public Column(String name) {
    myName = name;
  }

  public String getName() {
    return myName;
  }

  public Class<?> getValueClass() {
    return String.class;
  }

  public boolean isEditable() {
    return false;
  }

  public void setColumnValue(int iRow, Object value) {
    throw new UnsupportedOperationException();
  }

  public boolean needPack() {
    return false;
  }

  public abstract Object getColumnValue(T row);
}
