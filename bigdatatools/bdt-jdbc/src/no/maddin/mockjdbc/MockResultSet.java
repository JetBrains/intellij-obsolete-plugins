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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

public class MockResultSet implements ResultSet {
  private Record currentRecord;

  public MockResultSet() {
  }

  @Override
  public boolean next() {
    return false;
  }

  @Override
  public void close() {
    this.currentRecord = null;
  }

  @Override
  public boolean wasNull() {
    throw new UnsupportedOperationException("wasNull");
  }

  @Override
  public String getString(int columnIndex) {
    return "";
  }

  @Override
  public boolean getBoolean(int columnIndex) {
    throw new UnsupportedOperationException("getBoolean");
  }

  @Override
  public byte getByte(int columnIndex) {
    throw new UnsupportedOperationException();
  }

  @Override
  public short getShort(int columnIndex) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getInt(int columnIndex) {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getLong(int columnIndex) {
    throw new UnsupportedOperationException();
  }

  @Override
  public float getFloat(int columnIndex) {
    throw new UnsupportedOperationException();
  }

  @Override
  public double getDouble(int columnIndex) {
    throw new UnsupportedOperationException();
  }

  @Override
  public BigDecimal getBigDecimal(int columnIndex, int scale) {
    throw new UnsupportedOperationException("getBigDecimal");
  }

  @Override
  public byte[] getBytes(int columnIndex) {
    throw new UnsupportedOperationException("getBytes");
  }

  @Override
  public Date getDate(int columnIndex) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Time getTime(int columnIndex) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Timestamp getTimestamp(int columnIndex) {
    throw new UnsupportedOperationException();
  }

  @Override
  public InputStream getAsciiStream(int columnIndex) {
    throw new UnsupportedOperationException("getAsciiStream");
  }

  @Override
  public InputStream getUnicodeStream(int columnIndex) {
    throw new UnsupportedOperationException("getUnicodeStream");
  }

  @Override
  public InputStream getBinaryStream(int columnIndex) {
    throw new UnsupportedOperationException("getBinaryStream");
  }

  @Override
  public String getString(String columnLabel) throws SQLException {
    String result = check(currentRecord).get(columnLabel);
    if (result == null) {
      throw new SQLException("No Result for column: " + columnLabel);
    }
    return result;
  }

  @Override
  public boolean getBoolean(String columnLabel) {
    throw new UnsupportedOperationException("getBoolean");
  }

  @Override
  public byte getByte(String columnLabel) throws SQLException {
    String val = check(currentRecord).get(columnLabel);
    return Byte.parseByte(val);
  }

  @Override
  public short getShort(String columnLabel) throws SQLException {
    String val = check(currentRecord).get(columnLabel);
    return Short.parseShort(val);
  }

  @Override
  public int getInt(String columnLabel) throws SQLException {
    String val = check(currentRecord).get(columnLabel);
    return Integer.parseInt(val);
  }

  @Override
  public long getLong(String columnLabel) throws SQLException {
    String val = check(currentRecord).get(columnLabel);
    return Long.parseLong(val);
  }

  @Override
  public float getFloat(String columnLabel) throws SQLException {
    String val = check(currentRecord).get(columnLabel);
    return Float.parseFloat(val);
  }

  @Override
  public double getDouble(String columnLabel) throws SQLException {
    String val = check(currentRecord).get(columnLabel);
    return Double.parseDouble(val);
  }

  @Override
  public BigDecimal getBigDecimal(String columnLabel, int scale) {
    throw new UnsupportedOperationException("getBigDecimal");
  }

  @Override
  public byte[] getBytes(String columnLabel) {
    throw new UnsupportedOperationException("getBytes");
  }

  @Override
  public Date getDate(String columnLabel) throws SQLException {
    String val = check(currentRecord).get(columnLabel);
    LocalDate ld = LocalDate.parse(val, DateTimeFormatter.ISO_DATE);
    return Date.valueOf(ld);
  }

  @Override
  public Time getTime(String columnLabel) throws SQLException {
    String val = check(currentRecord).get(columnLabel);
    LocalTime lt = LocalTime.parse(val, DateTimeFormatter.ISO_TIME);
    return Time.valueOf(lt);
  }

  @Override
  public Timestamp getTimestamp(String columnLabel) throws SQLException {
    String val = check(currentRecord).get(columnLabel);
    LocalDateTime ldt = LocalDateTime.parse(val, DateTimeFormatter.ISO_DATE_TIME);
    return Timestamp.valueOf(ldt);
  }

  @Override
  public InputStream getAsciiStream(String columnLabel) {
    throw new UnsupportedOperationException("getAsciiStream");
  }

  @Override
  public InputStream getUnicodeStream(String columnLabel) {
    throw new UnsupportedOperationException("getUnicodeStream");
  }

  @Override
  public InputStream getBinaryStream(String columnLabel) {
    throw new UnsupportedOperationException("getBinaryStream");
  }

  @Override
  public SQLWarning getWarnings() {
    throw new UnsupportedOperationException("getWarnings");
  }

  @Override
  public void clearWarnings() {
    throw new UnsupportedOperationException("clearWarnings");
  }

  @Override
  public String getCursorName() {
    throw new UnsupportedOperationException("getCursorName");
  }

  @Override
  public ResultSetMetaData getMetaData() {
    return new MockResultSetMetaData();
  }

  @Override
  public Object getObject(int columnIndex) {
    throw new UnsupportedOperationException("getObject");
  }

  @Override
  public Object getObject(String columnLabel) {
    throw new UnsupportedOperationException("getObject");
  }

  @Override
  public int findColumn(String columnLabel) {
    throw new UnsupportedOperationException("findColumn");
  }

  @Override
  public Reader getCharacterStream(int columnIndex) {
    throw new UnsupportedOperationException("getCharacterStream");
  }

  @Override
  public Reader getCharacterStream(String columnLabel) {
    throw new UnsupportedOperationException("getCharacterStream");
  }

  @Override
  public BigDecimal getBigDecimal(int columnIndex) {
    throw new UnsupportedOperationException();
  }

  @Override
  public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
    String val = check(currentRecord).get(columnLabel);
    return BigDecimal.valueOf(Double.parseDouble(val));
  }

  @Override
  public boolean isBeforeFirst() {
    throw new UnsupportedOperationException("isBeforeFirst");
  }

  @Override
  public boolean isAfterLast() {
    throw new UnsupportedOperationException("isAfterLast");
  }

  @Override
  public boolean isFirst() {
    throw new UnsupportedOperationException("isFirst");
  }

  @Override
  public boolean isLast() {
    throw new UnsupportedOperationException("isLast");
  }

  @Override
  public void beforeFirst() {
    throw new UnsupportedOperationException("beforeFirst");
  }

  @Override
  public void afterLast() {
    throw new UnsupportedOperationException("afterLast");
  }

  @Override
  public boolean first() {
    throw new UnsupportedOperationException("first");
  }

  @Override
  public boolean last() {
    throw new UnsupportedOperationException("last");
  }

  @Override
  public int getRow() {
    throw new UnsupportedOperationException("getRow");
  }

  @Override
  public boolean absolute(int row) {
    throw new UnsupportedOperationException("absolute");
  }

  @Override
  public boolean relative(int rows) {
    throw new UnsupportedOperationException("relative");
  }

  @Override
  public boolean previous() {
    throw new UnsupportedOperationException("previous");
  }

  @Override
  public int getFetchDirection() {
    throw new UnsupportedOperationException("getFetchDirection");
  }

  @Override
  public void setFetchDirection(int direction) {
    throw new UnsupportedOperationException("setFetchDirection");
  }

  @Override
  public int getFetchSize() {
    throw new UnsupportedOperationException("getFetchSize");
  }

  @Override
  public void setFetchSize(int rows) {
    throw new UnsupportedOperationException("setFetchSize");
  }

  @Override
  public int getType() {
    return TYPE_FORWARD_ONLY;
  }

  @Override
  public int getConcurrency() {
    return CONCUR_UPDATABLE;
  }

  @Override
  public boolean rowUpdated() {
    return false;
  }

  @Override
  public boolean rowInserted() {
    return false;
  }

  @Override
  public boolean rowDeleted() {
    return false;
  }

  @Override
  public void updateNull(int columnIndex) {
  }

  @Override
  public void updateBoolean(int columnIndex, boolean x) {
  }

  @Override
  public void updateByte(int columnIndex, byte x) {
  }

  @Override
  public void updateShort(int columnIndex, short x) {
  }

  @Override
  public void updateInt(int columnIndex, int x) {
  }

  @Override
  public void updateLong(int columnIndex, long x) {
  }

  @Override
  public void updateFloat(int columnIndex, float x) {
  }

  @Override
  public void updateDouble(int columnIndex, double x) {
  }

  @Override
  public void updateBigDecimal(int columnIndex, BigDecimal x) {
  }

  @Override
  public void updateString(int columnIndex, String x) {
  }

  @Override
  public void updateBytes(int columnIndex, byte[] x) {
  }

  @Override
  public void updateDate(int columnIndex, Date x) {
  }

  @Override
  public void updateTime(int columnIndex, Time x) {
  }

  @Override
  public void updateTimestamp(int columnIndex, Timestamp x) {
    throw new UnsupportedOperationException("updateTimestamp");
  }

  @Override
  public void updateAsciiStream(int columnIndex, InputStream x, int length) {
  }

  @Override
  public void updateBinaryStream(int columnIndex, InputStream x, int length) {
  }

  @Override
  public void updateCharacterStream(int columnIndex, Reader x, int length) {
  }

  @Override
  public void updateObject(int columnIndex, Object x, int scaleOrLength) {
  }

  @Override
  public void updateObject(int columnIndex, Object x) {
  }

  @Override
  public void updateNull(String columnLabel) {
  }

  @Override
  public void updateBoolean(String columnLabel, boolean x) {
  }

  @Override
  public void updateByte(String columnLabel, byte x) {
  }

  @Override
  public void updateShort(String columnLabel, short x) {
  }

  @Override
  public void updateInt(String columnLabel, int x) {
  }

  @Override
  public void updateLong(String columnLabel, long x) {
  }

  @Override
  public void updateFloat(String columnLabel, float x) {
  }

  @Override
  public void updateDouble(String columnLabel, double x) {
  }

  @Override
  public void updateBigDecimal(String columnLabel, BigDecimal x) {
  }

  @Override
  public void updateString(String columnLabel, String x) {
  }

  @Override
  public void updateBytes(String columnLabel, byte[] x) {
  }

  @Override
  public void updateDate(String columnLabel, Date x) {
  }

  @Override
  public void updateTime(String columnLabel, Time x) {
  }

  @Override
  public void updateTimestamp(String columnLabel, Timestamp x) {
  }

  @Override
  public void updateAsciiStream(String columnLabel, InputStream x, int length) {
  }

  @Override
  public void updateBinaryStream(String columnLabel, InputStream x, int length) {
  }

  @Override
  public void updateCharacterStream(String columnLabel, Reader reader, int length) {
  }

  @Override
  public void updateObject(String columnLabel, Object x, int scaleOrLength) {
  }

  @Override
  public void updateObject(String columnLabel, Object x) {
  }

  @Override
  public void insertRow() {
  }

  @Override
  public void updateRow() {
  }

  @Override
  public void deleteRow() {
  }

  @Override
  public void refreshRow() {
  }

  @Override
  public void cancelRowUpdates() {
  }

  @Override
  public void moveToInsertRow() {
  }

  @Override
  public void moveToCurrentRow() {
  }

  @Override
  public Statement getStatement() {
    throw new UnsupportedOperationException("getStatement");
  }

  @Override
  public Object getObject(int columnIndex, Map<String, Class<?>> map) {
    throw new UnsupportedOperationException("getObject");
  }

  @Override
  public Ref getRef(int columnIndex) {
    throw new UnsupportedOperationException("getRef");
  }

  @Override
  public Blob getBlob(int columnIndex) {
    throw new UnsupportedOperationException("getBlob");
  }

  @Override
  public Clob getClob(int columnIndex) {
    throw new UnsupportedOperationException("getClob");
  }

  @Override
  public Array getArray(int columnIndex) {
    throw new UnsupportedOperationException("getArray");
  }

  @Override
  public Object getObject(String columnLabel, Map<String, Class<?>> map) {
    throw new UnsupportedOperationException("getObject");
  }

  @Override
  public Ref getRef(String columnLabel) {
    throw new UnsupportedOperationException("getRef");
  }

  @Override
  public Blob getBlob(String columnLabel) {
    throw new UnsupportedOperationException("getBlob");
  }

  @Override
  public Clob getClob(String columnLabel) {
    throw new UnsupportedOperationException("getClob");
  }

  @Override
  public Array getArray(String columnLabel) {
    throw new UnsupportedOperationException("getArray");
  }

  @Override
  public Date getDate(int columnIndex, Calendar cal) {
    throw new UnsupportedOperationException("getDate");
  }

  @Override
  public Date getDate(String columnLabel, Calendar cal) {
    throw new UnsupportedOperationException("getDate");
  }

  @Override
  public Time getTime(int columnIndex, Calendar cal) {
    throw new UnsupportedOperationException("getTime");
  }

  @Override
  public Time getTime(String columnLabel, Calendar cal) {
    throw new UnsupportedOperationException("getTime");
  }

  @Override
  public Timestamp getTimestamp(int columnIndex, Calendar cal) {
    throw new UnsupportedOperationException("getTimestamp");
  }

  @Override
  public Timestamp getTimestamp(String columnLabel, Calendar cal) {
    throw new UnsupportedOperationException("getTimestamp");
  }

  @Override
  public URL getURL(int columnIndex) {
    throw new UnsupportedOperationException("getURL");
  }

  @Override
  public URL getURL(String columnLabel) {
    throw new UnsupportedOperationException("getURL");
  }

  @Override
  public void updateRef(int columnIndex, Ref x) {
    throw new UnsupportedOperationException("updateRef");
  }

  @Override
  public void updateRef(String columnLabel, Ref x) {
    throw new UnsupportedOperationException("updateRef");
  }

  @Override
  public void updateBlob(int columnIndex, Blob x) {
    throw new UnsupportedOperationException("updateBlob");
  }

  @Override
  public void updateBlob(String columnLabel, Blob x) {
    throw new UnsupportedOperationException("updateBlob");
  }

  @Override
  public void updateClob(int columnIndex, Clob x) {
    throw new UnsupportedOperationException("updateClob");
  }

  @Override
  public void updateClob(String columnLabel, Clob x) {
    throw new UnsupportedOperationException("updateClob");
  }

  @Override
  public void updateArray(int columnIndex, Array x) {
    throw new UnsupportedOperationException("updateArray");
  }

  @Override
  public void updateArray(String columnLabel, Array x) {
    throw new UnsupportedOperationException("updateArray");
  }

  @Override
  public RowId getRowId(int columnIndex) {
    throw new UnsupportedOperationException("getRowId");
  }

  @Override
  public RowId getRowId(String columnLabel) {
    throw new UnsupportedOperationException("getRowId");
  }

  @Override
  public void updateRowId(int columnIndex, RowId x) {
    throw new UnsupportedOperationException("updateRowId");
  }

  @Override
  public void updateRowId(String columnLabel, RowId x) {
    throw new UnsupportedOperationException("updateRowId");
  }

  @Override
  public int getHoldability() {
    throw new UnsupportedOperationException("getHoldability");
  }

  @Override
  public boolean isClosed() {
    throw new UnsupportedOperationException("isClosed");
  }

  @Override
  public void updateNString(int columnIndex, String nString) {
    throw new UnsupportedOperationException("updateNString");
  }

  @Override
  public void updateNString(String columnLabel, String nString) {
    throw new UnsupportedOperationException("updateNString");
  }

  @Override
  public void updateNClob(int columnIndex, NClob nClob) {
    throw new UnsupportedOperationException("updateNClob");
  }

  @Override
  public void updateNClob(String columnLabel, NClob nClob) {
    throw new UnsupportedOperationException("updateNClob");
  }

  @Override
  public NClob getNClob(int columnIndex) {
    throw new UnsupportedOperationException("getNClob");
  }

  @Override
  public NClob getNClob(String columnLabel) {
    throw new UnsupportedOperationException("getNClob");
  }

  @Override
  public SQLXML getSQLXML(int columnIndex) {
    throw new UnsupportedOperationException("getSQLXML");
  }

  @Override
  public SQLXML getSQLXML(String columnLabel) {
    throw new UnsupportedOperationException("getSQLXML");
  }

  @Override
  public void updateSQLXML(int columnIndex, SQLXML xmlObject) {
    throw new UnsupportedOperationException("updateSQLXML");
  }

  @Override
  public void updateSQLXML(String columnLabel, SQLXML xmlObject) {
    throw new UnsupportedOperationException("updateSQLXML");
  }

  @Override
  public String getNString(int columnIndex) {
    throw new UnsupportedOperationException("getNString");
  }

  @Override
  public String getNString(String columnLabel) {
    throw new UnsupportedOperationException("getNString");
  }

  @Override
  public Reader getNCharacterStream(int columnIndex) {
    throw new UnsupportedOperationException("getNCharacterStream");
  }

  @Override
  public Reader getNCharacterStream(String columnLabel) {
    throw new UnsupportedOperationException("getNCharacterStream");
  }

  @Override
  public void updateNCharacterStream(int columnIndex, Reader x, long length) {
    throw new UnsupportedOperationException("updateNCharacterStream");
  }

  @Override
  public void updateNCharacterStream(String columnLabel, Reader reader, long length) {
    throw new UnsupportedOperationException("updateNCharacterStream");
  }

  @Override
  public void updateAsciiStream(int columnIndex, InputStream x, long length) {
    throw new UnsupportedOperationException("updateAsciiStream");
  }

  @Override
  public void updateBinaryStream(int columnIndex, InputStream x, long length) {
    throw new UnsupportedOperationException("updateBinaryStream");
  }

  @Override
  public void updateCharacterStream(int columnIndex, Reader x, long length) {
    throw new UnsupportedOperationException("updateCharacterStream");
  }

  @Override
  public void updateAsciiStream(String columnLabel, InputStream x, long length) {
    throw new UnsupportedOperationException("updateAsciiStream");
  }

  @Override
  public void updateBinaryStream(String columnLabel, InputStream x, long length) {
    throw new UnsupportedOperationException("updateBinaryStream");
  }

  @Override
  public void updateCharacterStream(String columnLabel, Reader reader, long length) {
    throw new UnsupportedOperationException("updateCharacterStream");
  }

  @Override
  public void updateBlob(int columnIndex, InputStream inputStream, long length) {
    throw new UnsupportedOperationException("updateBlob");
  }

  @Override
  public void updateBlob(String columnLabel, InputStream inputStream, long length) {
    throw new UnsupportedOperationException("updateBlob");
  }

  @Override
  public void updateClob(int columnIndex, Reader reader, long length) {
    throw new UnsupportedOperationException("updateClob");
  }

  @Override
  public void updateClob(String columnLabel, Reader reader, long length) {
    throw new UnsupportedOperationException("updateClob");
  }

  @Override
  public void updateNClob(int columnIndex, Reader reader, long length) {
    throw new UnsupportedOperationException("updateNClob");
  }

  @Override
  public void updateNClob(String columnLabel, Reader reader, long length) {
    throw new UnsupportedOperationException("updateNClob");
  }

  @Override
  public void updateNCharacterStream(int columnIndex, Reader x) {
    throw new UnsupportedOperationException("updateNCharacterStream");
  }

  @Override
  public void updateNCharacterStream(String columnLabel, Reader reader) {
    throw new UnsupportedOperationException("updateNCharacterStream");
  }

  @Override
  public void updateAsciiStream(int columnIndex, InputStream x) {
    throw new UnsupportedOperationException("updateAsciiStream");
  }

  @Override
  public void updateBinaryStream(int columnIndex, InputStream x) {
    throw new UnsupportedOperationException("updateBinaryStream");
  }

  @Override
  public void updateCharacterStream(int columnIndex, Reader x) {
    throw new UnsupportedOperationException("updateCharacterStream");
  }

  @Override
  public void updateAsciiStream(String columnLabel, InputStream x) {
    throw new UnsupportedOperationException("updateAsciiStream");
  }

  @Override
  public void updateBinaryStream(String columnLabel, InputStream x) {
    throw new UnsupportedOperationException("updateBinaryStream");
  }

  @Override
  public void updateCharacterStream(String columnLabel, Reader reader) {
    throw new UnsupportedOperationException("updateCharacterStream");
  }

  @Override
  public void updateBlob(int columnIndex, InputStream inputStream) {
    throw new UnsupportedOperationException("updateBlob");
  }

  @Override
  public void updateBlob(String columnLabel, InputStream inputStream) {
    throw new UnsupportedOperationException("updateBlob");
  }

  @Override
  public void updateClob(int columnIndex, Reader reader) {
    throw new UnsupportedOperationException("updateClob");
  }

  @Override
  public void updateClob(String columnLabel, Reader reader) {
    throw new UnsupportedOperationException("updateClob");
  }

  @Override
  public void updateNClob(int columnIndex, Reader reader) {
    throw new UnsupportedOperationException("updateNClob");
  }

  @Override
  public void updateNClob(String columnLabel, Reader reader) {
    throw new UnsupportedOperationException("updateNClob");
  }

  @Override
  public <T> T getObject(int columnIndex, Class<T> type) {
    throw new UnsupportedOperationException("getObject");
  }

  @Override
  public <T> T getObject(String columnLabel, Class<T> type) {
    throw new UnsupportedOperationException("getObject");
  }

  @Override
  public <T> T unwrap(Class<T> iface) {
    throw new UnsupportedOperationException("unwrap");
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) {
    throw new UnsupportedOperationException("isWrapperFor");
  }

  private static Record check(Record record) throws SQLException {
    if (record == null) {
      throw new SQLException("No current record");
    }
    return record;
  }

  private static class Record extends LinkedHashMap<String, String> {
    private Record() {
    }
  }
}
