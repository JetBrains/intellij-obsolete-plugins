/*
 * Derived from mock-jdbc by Martin Goldhahn:
 * https://github.com/maddingo/mockjdbc
 *
 * Licensed under the Apache License, Version 2.0.
 * See the repository LICENSE file for details.
 *
 * Modifications Copyright 2000-2026 JetBrains s.r.o. and contributors.
 */

package no.maddin.mockjdbc;

import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.LinkedHashMap;

class MockResultSetMetaData implements ResultSetMetaData {
  private static final int DEFAULT_COLUMN_PRECISION = 60;
  private static final int DEFAULT_COLUMN_SCALE = 0;
  private static final String tableName = "mock";
  private final String[] columnNames = new String[]{"One", "Two", "Three"};
  private final LinkedHashMap<String, DataType> columnTypes = new LinkedHashMap<>() {{
    put("One", DataType.VARCHAR);
    put("Two", DataType.VARCHAR);
    put("Three", DataType.VARCHAR);
  }};

  MockResultSetMetaData() { }

  @Override
  public int getColumnCount() {
    return columnTypes.size();
  }

  @Override
  public boolean isAutoIncrement(int column) {
    throw new UnsupportedOperationException("isAutoIncrement");
  }

  @Override
  public boolean isCaseSensitive(int column) {
    throw new UnsupportedOperationException("isCaseSensitive");
  }

  @Override
  public boolean isSearchable(int column) {
    throw new UnsupportedOperationException("isSearchable");
  }

  @Override
  public boolean isCurrency(int column) {
    throw new UnsupportedOperationException("isCurrency");
  }

  @Override
  public int isNullable(int column) {
    throw new UnsupportedOperationException("isNullable");
  }

  @Override
  public boolean isSigned(int column) {
    throw new UnsupportedOperationException("isSigned");
  }

  @Override
  public int getColumnDisplaySize(int column) {
    throw new UnsupportedOperationException("getColumnDisplaySize");
  }

  @Override
  public String getColumnLabel(int column) {
    return getColumnName(column);
  }

  @Override
  public String getColumnName(int column) {
    return columnNames[column - 1];
  }

  @Override
  public String getSchemaName(int column) {
    return "";
  }

  @Override
  public int getPrecision(int column) {
    return DEFAULT_COLUMN_PRECISION;
  }

  @Override
  public int getScale(int column) {
    return DEFAULT_COLUMN_SCALE;
  }

  @Override
  public String getTableName(int column) {
    return tableName;
  }

  @Override
  public String getCatalogName(int column) {
    throw new UnsupportedOperationException("getCatalogName");
  }

  @Override
  public int getColumnType(int column) {
    return columnTypes.get(columnNames[column - 1]).sqlType;
  }

  @Override
  public String getColumnTypeName(int column) {
    return columnTypes.get(columnNames[column - 1]).name();
  }

  @Override
  public boolean isReadOnly(int column) {
    throw new UnsupportedOperationException("isReadOnly");
  }

  @Override
  public boolean isWritable(int column) {
    throw new UnsupportedOperationException("isWritable");
  }

  @Override
  public boolean isDefinitelyWritable(int column) {
    throw new UnsupportedOperationException("isDefinitelyWritable");
  }

  @Override
  public String getColumnClassName(int column) {
    throw new UnsupportedOperationException("getColumnClassName");
  }

  @Override
  public <T> T unwrap(Class<T> iface) {
    throw new UnsupportedOperationException("unwrap");
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) {
    throw new UnsupportedOperationException("isWrapperFor");
  }

  private enum DataType {
    VARCHAR(Types.VARCHAR),
    INTEGER(Types.INTEGER),
    DOUBLE(Types.DOUBLE),
    DATE(Types.DATE),
    TIME(Types.TIME),
    TIMESTAMP(Types.TIMESTAMP);

    private final int sqlType;

    DataType(int sqlType) {
      this.sqlType = sqlType;
    }
  }
}
