/*
 * Derived from mock-jdbc by Martin Goldhahn:
 * https://github.com/maddingo/mockjdbc
 *
 * Licensed under the Apache License, Version 2.0.
 * See the repository LICENSE file for details.
 *
 * Modifications Copyright 2000-2026 JetBrains s.r.o. and contributors.
 */

package com.dbschema;

import no.maddin.mockjdbc.MockResultSet;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class MockStatement implements CallableStatement {

  private final AtomicBoolean isClosed = new AtomicBoolean(true);
  private SQLWarning warnings;
  private MockResultSet currentResultSet;

  MockStatement(String sql) {
    if (sql != null) {
      openCurrentFile(sql);
    }
  }

  private synchronized void openCurrentFile(String sql) {
    isClosed.set(false);
  }

  @Override
  public void registerOutParameter(int parameterIndex, int sqlType) {
    throw new UnsupportedOperationException("registerOutParameter");
  }

  @Override
  public void registerOutParameter(int parameterIndex, int sqlType, int scale) {
    throw new UnsupportedOperationException("registerOutParameter");
  }

  @Override
  public boolean wasNull() {
    throw new UnsupportedOperationException("wasNull");
  }

  @Override
  public String getString(int parameterIndex) {
    throw new UnsupportedOperationException("getString");
  }

  @Override
  public boolean getBoolean(int parameterIndex) {
    throw new UnsupportedOperationException("getBoolean");
  }

  @Override
  public byte getByte(int parameterIndex) {
    throw new UnsupportedOperationException("getByte");
  }

  @Override
  public short getShort(int parameterIndex) {
    throw new UnsupportedOperationException("getShort");
  }

  @Override
  public int getInt(int parameterIndex) {
    throw new UnsupportedOperationException("getInt");
  }

  @Override
  public long getLong(int parameterIndex) {
    throw new UnsupportedOperationException("getLong");
  }

  @Override
  public float getFloat(int parameterIndex) {
    throw new UnsupportedOperationException("getFloat");
  }

  @Override
  public double getDouble(int parameterIndex) {
    throw new UnsupportedOperationException("getDouble");
  }

  @Override
  public BigDecimal getBigDecimal(int parameterIndex, int scale) {
    throw new UnsupportedOperationException("getBigDecimal");
  }

  @Override
  public byte[] getBytes(int parameterIndex) {
    throw new UnsupportedOperationException("getBytes");
  }

  @Override
  public Date getDate(int parameterIndex) {
    throw new UnsupportedOperationException("getDate");
  }

  @Override
  public Time getTime(int parameterIndex) {
    throw new UnsupportedOperationException("getTime");
  }

  @Override
  public Timestamp getTimestamp(int parameterIndex) {
    throw new UnsupportedOperationException("getTimestamp");
  }

  @Override
  public Object getObject(int parameterIndex) {
    throw new UnsupportedOperationException("getObject");
  }

  @Override
  public BigDecimal getBigDecimal(int parameterIndex) {
    throw new UnsupportedOperationException("getBigDecimal");
  }

  @Override
  public Object getObject(int parameterIndex, Map<String, Class<?>> map) {
    throw new UnsupportedOperationException("getObject");
  }

  @Override
  public Ref getRef(int parameterIndex) {
    throw new UnsupportedOperationException("getRef");
  }

  @Override
  public Blob getBlob(int parameterIndex) {
    throw new UnsupportedOperationException("getBlob");
  }

  @Override
  public Clob getClob(int parameterIndex) {
    throw new UnsupportedOperationException("getClob");
  }

  @Override
  public Array getArray(int parameterIndex) {
    throw new UnsupportedOperationException("getArray");
  }

  @Override
  public Date getDate(int parameterIndex, Calendar cal) {
    throw new UnsupportedOperationException("getDate");
  }

  @Override
  public Time getTime(int parameterIndex, Calendar cal) {
    throw new UnsupportedOperationException("getTime");
  }

  @Override
  public Timestamp getTimestamp(int parameterIndex, Calendar cal) {
    throw new UnsupportedOperationException("getTimestamp");
  }

  @Override
  public void registerOutParameter(int parameterIndex, int sqlType, String typeName) {
    throw new UnsupportedOperationException("registerOutParameter");
  }

  @Override
  public void registerOutParameter(String parameterName, int sqlType) {
    throw new UnsupportedOperationException("registerOutParameter");
  }

  @Override
  public void registerOutParameter(String parameterName, int sqlType, int scale) {
    throw new UnsupportedOperationException("registerOutParameter");
  }

  @Override
  public void registerOutParameter(String parameterName, int sqlType, String typeName) {
    throw new UnsupportedOperationException("registerOutParameter");
  }

  @Override
  public URL getURL(int parameterIndex) {
    throw new UnsupportedOperationException("getURL");
  }

  @Override
  public void setURL(String parameterName, URL val) {
    throw new UnsupportedOperationException("setURL");
  }

  @Override
  public void setNull(String parameterName, int sqlType) {
    throw new UnsupportedOperationException("setNull");
  }

  @Override
  public void setBoolean(String parameterName, boolean x) {
    throw new UnsupportedOperationException("setBoolean");
  }

  @Override
  public void setByte(String parameterName, byte x) {
    throw new UnsupportedOperationException("setByte");
  }

  @Override
  public void setShort(String parameterName, short x) {
    throw new UnsupportedOperationException("setShort");
  }

  @Override
  public void setInt(String parameterName, int x) {
    throw new UnsupportedOperationException("setInt");
  }

  @Override
  public void setLong(String parameterName, long x) {
    throw new UnsupportedOperationException("setLong");
  }

  @Override
  public void setFloat(String parameterName, float x) {
    throw new UnsupportedOperationException("setFloat");
  }

  @Override
  public void setDouble(String parameterName, double x) {
    throw new UnsupportedOperationException("setDouble");
  }

  @Override
  public void setBigDecimal(String parameterName, BigDecimal x) {
    throw new UnsupportedOperationException("setBigDecimal");
  }

  @Override
  public void setString(String parameterName, String x) {
    throw new UnsupportedOperationException("setString");
  }

  @Override
  public void setBytes(String parameterName, byte[] x) {
    throw new UnsupportedOperationException("setBytes");
  }

  @Override
  public void setDate(String parameterName, Date x) {
    throw new UnsupportedOperationException("setDate");
  }

  @Override
  public void setTime(String parameterName, Time x) {
    throw new UnsupportedOperationException("setTime");
  }

  @Override
  public void setTimestamp(String parameterName, Timestamp x) {
    throw new UnsupportedOperationException("setTimestamp");
  }

  @Override
  public void setAsciiStream(String parameterName, InputStream x, int length) {
    throw new UnsupportedOperationException("setAsciiStream");
  }

  @Override
  public void setBinaryStream(String parameterName, InputStream x, int length) {
    throw new UnsupportedOperationException("setBinaryStream");
  }

  @Override
  public void setObject(String parameterName, Object x, int targetSqlType, int scale) {
    throw new UnsupportedOperationException("setObject");
  }

  @Override
  public void setObject(String parameterName, Object x, int targetSqlType) {
    throw new UnsupportedOperationException("setObject");
  }

  @Override
  public void setObject(String parameterName, Object x) {
    throw new UnsupportedOperationException("setObject");
  }

  @Override
  public void setCharacterStream(String parameterName, Reader reader, int length) {
    throw new UnsupportedOperationException("setCharacterStream");
  }

  @Override
  public void setDate(String parameterName, Date x, Calendar cal) {
    throw new UnsupportedOperationException("setDate");
  }

  @Override
  public void setTime(String parameterName, Time x, Calendar cal) {
    throw new UnsupportedOperationException("setTime");
  }

  @Override
  public void setTimestamp(String parameterName, Timestamp x, Calendar cal) {
    throw new UnsupportedOperationException("setTimestamp");
  }

  @Override
  public void setNull(String parameterName, int sqlType, String typeName) {
    throw new UnsupportedOperationException("setNull");
  }

  @Override
  public String getString(String parameterName) {
    throw new UnsupportedOperationException("getString");
  }

  @Override
  public boolean getBoolean(String parameterName) {
    throw new UnsupportedOperationException("getBoolean");
  }

  @Override
  public byte getByte(String parameterName) {
    throw new UnsupportedOperationException("getByte");
  }

  @Override
  public short getShort(String parameterName) {
    throw new UnsupportedOperationException("getShort");
  }

  @Override
  public int getInt(String parameterName) {
    throw new UnsupportedOperationException("getInt");
  }

  @Override
  public long getLong(String parameterName) {
    throw new UnsupportedOperationException("getLong");
  }

  @Override
  public float getFloat(String parameterName) {
    throw new UnsupportedOperationException("getFloat");
  }

  @Override
  public double getDouble(String parameterName) {
    throw new UnsupportedOperationException("getDouble");
  }

  @Override
  public byte[] getBytes(String parameterName) {
    throw new UnsupportedOperationException("getBytes");
  }

  @Override
  public Date getDate(String parameterName) {
    throw new UnsupportedOperationException("getDate");
  }

  @Override
  public Time getTime(String parameterName) {
    throw new UnsupportedOperationException("getTime");
  }

  @Override
  public Timestamp getTimestamp(String parameterName) {
    throw new UnsupportedOperationException("getTimestamp");
  }

  @Override
  public Object getObject(String parameterName) {
    throw new UnsupportedOperationException("getObject");
  }

  @Override
  public BigDecimal getBigDecimal(String parameterName) {
    throw new UnsupportedOperationException("getBigDecimal");
  }

  @Override
  public Object getObject(String parameterName, Map<String, Class<?>> map) {
    throw new UnsupportedOperationException("getObject");
  }

  @Override
  public Ref getRef(String parameterName) {
    throw new UnsupportedOperationException("getRef");
  }

  @Override
  public Blob getBlob(String parameterName) {
    throw new UnsupportedOperationException("getBlob");
  }

  @Override
  public Clob getClob(String parameterName) {
    throw new UnsupportedOperationException("getClob");
  }

  @Override
  public Array getArray(String parameterName) {
    throw new UnsupportedOperationException("getArray");
  }

  @Override
  public Date getDate(String parameterName, Calendar cal) {
    throw new UnsupportedOperationException("getDate");
  }

  @Override
  public Time getTime(String parameterName, Calendar cal) {
    throw new UnsupportedOperationException("getTime");
  }

  @Override
  public Timestamp getTimestamp(String parameterName, Calendar cal) {
    throw new UnsupportedOperationException("getTimestamp");
  }

  @Override
  public URL getURL(String parameterName) {
    throw new UnsupportedOperationException("getURL");
  }

  @Override
  public RowId getRowId(int parameterIndex) {
    throw new UnsupportedOperationException("getRowId");
  }

  @Override
  public RowId getRowId(String parameterName) {
    throw new UnsupportedOperationException("getRowId");
  }

  @Override
  public void setRowId(String parameterName, RowId x) {
    throw new UnsupportedOperationException("setRowId");
  }

  @Override
  public void setNString(String parameterName, String value) {
    throw new UnsupportedOperationException("setNString");
  }

  @Override
  public void setNCharacterStream(String parameterName, Reader value, long length) {
    throw new UnsupportedOperationException("setNCharacterStream");
  }

  @Override
  public void setNClob(String parameterName, NClob value) {
    throw new UnsupportedOperationException("setNClob");
  }

  @Override
  public void setClob(String parameterName, Reader reader, long length) {
    throw new UnsupportedOperationException("setClob");
  }

  @Override
  public void setBlob(String parameterName, InputStream inputStream, long length) {
    throw new UnsupportedOperationException("setBlob");
  }

  @Override
  public void setNClob(String parameterName, Reader reader, long length) {
    throw new UnsupportedOperationException("setNClob");
  }

  @Override
  public NClob getNClob(int parameterIndex) {
    throw new UnsupportedOperationException("getNClob");
  }

  @Override
  public NClob getNClob(String parameterName) {
    throw new UnsupportedOperationException("getNClob");
  }

  @Override
  public void setSQLXML(String parameterName, SQLXML xmlObject) {
    throw new UnsupportedOperationException("setSQLXML");
  }

  @Override
  public SQLXML getSQLXML(int parameterIndex) {
    throw new UnsupportedOperationException("getSQLXML");
  }

  @Override
  public SQLXML getSQLXML(String parameterName) {
    throw new UnsupportedOperationException("getSQLXML");
  }

  @Override
  public String getNString(int parameterIndex) {
    throw new UnsupportedOperationException("getNString");
  }

  @Override
  public String getNString(String parameterName) {
    throw new UnsupportedOperationException("getNString");
  }

  @Override
  public Reader getNCharacterStream(int parameterIndex) {
    throw new UnsupportedOperationException("getNCharacterStream");
  }

  @Override
  public Reader getNCharacterStream(String parameterName) {
    throw new UnsupportedOperationException("getNCharacterStream");
  }

  @Override
  public Reader getCharacterStream(int parameterIndex) {
    throw new UnsupportedOperationException("getCharacterStream");
  }

  @Override
  public Reader getCharacterStream(String parameterName) {
    throw new UnsupportedOperationException("getCharacterStream");
  }

  @Override
  public void setBlob(String parameterName, Blob x) {
    throw new UnsupportedOperationException("setBlob");
  }

  @Override
  public void setClob(String parameterName, Clob x) {
    throw new UnsupportedOperationException("setClob");
  }

  @Override
  public void setAsciiStream(String parameterName, InputStream x, long length) {
    throw new UnsupportedOperationException("setAsciiStream");
  }

  @Override
  public void setBinaryStream(String parameterName, InputStream x, long length) {
    throw new UnsupportedOperationException("setBinaryStream");
  }

  @Override
  public void setCharacterStream(String parameterName, Reader reader, long length) {
    throw new UnsupportedOperationException("setCharacterStream");
  }

  @Override
  public void setAsciiStream(String parameterName, InputStream x) {
    throw new UnsupportedOperationException("setAsciiStream");
  }

  @Override
  public void setBinaryStream(String parameterName, InputStream x) {
    throw new UnsupportedOperationException("setBinaryStream");
  }

  @Override
  public void setCharacterStream(String parameterName, Reader reader) {
    throw new UnsupportedOperationException("setCharacterStream");
  }

  @Override
  public void setNCharacterStream(String parameterName, Reader value) {
    throw new UnsupportedOperationException("setNCharacterStream");
  }

  @Override
  public void setClob(String parameterName, Reader reader) {
    throw new UnsupportedOperationException("setClob");
  }

  @Override
  public void setBlob(String parameterName, InputStream inputStream) {
    throw new UnsupportedOperationException("setBlob");
  }

  @Override
  public void setNClob(String parameterName, Reader reader) {
    throw new UnsupportedOperationException("setNClob");
  }

  @Override
  public <T> T getObject(int parameterIndex, Class<T> type) {
    throw new UnsupportedOperationException("getObject");
  }

  @Override
  public <T> T getObject(String parameterName, Class<T> type) {
    throw new UnsupportedOperationException("getObject");
  }

  @Override
  public ResultSet executeQuery() {
    return new MockResultSet();
  }

  @Override
  public int executeUpdate() {
    throw new UnsupportedOperationException("executeUpdate");
  }

  @Override
  public void setNull(int parameterIndex, int sqlType) {
    throw new UnsupportedOperationException("setNull");
  }

  @Override
  public void setBoolean(int parameterIndex, boolean x) {
    throw new UnsupportedOperationException("setBoolean");
  }

  @Override
  public void setByte(int parameterIndex, byte x) {
    throw new UnsupportedOperationException("setByte");
  }

  @Override
  public void setShort(int parameterIndex, short x) {
    throw new UnsupportedOperationException("setShort");
  }

  @Override
  public void setInt(int parameterIndex, int x) {
    throw new UnsupportedOperationException("setInt");
  }

  @Override
  public void setLong(int parameterIndex, long x) {
    throw new UnsupportedOperationException("setLong");
  }

  @Override
  public void setFloat(int parameterIndex, float x) {
    throw new UnsupportedOperationException("setFloat");
  }

  @Override
  public void setDouble(int parameterIndex, double x) {
    throw new UnsupportedOperationException("setDouble");
  }

  @Override
  public void setBigDecimal(int parameterIndex, BigDecimal x) {
    throw new UnsupportedOperationException("setBigDecimal");
  }

  @Override
  public void setString(int parameterIndex, String x) {
    throw new UnsupportedOperationException("setString");
  }

  @Override
  public void setBytes(int parameterIndex, byte[] x) {
    throw new UnsupportedOperationException("setBytes");
  }

  @Override
  public void setDate(int parameterIndex, Date x) {
    throw new UnsupportedOperationException("setDate");
  }

  @Override
  public void setTime(int parameterIndex, Time x) {
    throw new UnsupportedOperationException("setTime");
  }

  @Override
  public void setTimestamp(int parameterIndex, Timestamp x) {
    throw new UnsupportedOperationException("setTimestamp");
  }

  @Override
  public void setAsciiStream(int parameterIndex, InputStream x, int length) {
    throw new UnsupportedOperationException("setAsciiStream");
  }

  @Override
  public void setUnicodeStream(int parameterIndex, InputStream x, int length) {
    throw new UnsupportedOperationException("setUnicodeStream");
  }

  @Override
  public void setBinaryStream(int parameterIndex, InputStream x, int length) {
    throw new UnsupportedOperationException("setBinaryStream");
  }

  @Override
  public void clearParameters() {
    throw new UnsupportedOperationException("clearParameters");
  }

  @Override
  public void setObject(int parameterIndex, Object x, int targetSqlType) {
    throw new UnsupportedOperationException("setObject");
  }

  @Override
  public void setObject(int parameterIndex, Object x) {
    throw new UnsupportedOperationException("setObject");
  }

  @Override
  public boolean execute() {
    currentResultSet = new MockResultSet();
    return true;
  }

  @Override
  public void addBatch() {

  }

  @Override
  public void setCharacterStream(int parameterIndex, Reader reader, int length) {

  }

  @Override
  public void setRef(int parameterIndex, Ref x) {

  }

  @Override
  public void setBlob(int parameterIndex, Blob x) {

  }

  @Override
  public void setClob(int parameterIndex, Clob x) {

  }

  @Override
  public void setArray(int parameterIndex, Array x) {

  }

  @Override
  public ResultSetMetaData getMetaData() {
    return null;
  }

  @Override
  public void setDate(int parameterIndex, Date x, Calendar cal) {

  }

  @Override
  public void setTime(int parameterIndex, Time x, Calendar cal) {

  }

  @Override
  public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) {

  }

  @Override
  public void setNull(int parameterIndex, int sqlType, String typeName) {

  }

  @Override
  public void setURL(int parameterIndex, URL x) {

  }

  @Override
  public ParameterMetaData getParameterMetaData() {
    return null;
  }

  @Override
  public void setRowId(int parameterIndex, RowId x) {

  }

  @Override
  public void setNString(int parameterIndex, String value) {

  }

  @Override
  public void setNCharacterStream(int parameterIndex, Reader value, long length) {

  }

  @Override
  public void setNClob(int parameterIndex, NClob value) {

  }

  @Override
  public void setClob(int parameterIndex, Reader reader, long length) {

  }

  @Override
  public void setBlob(int parameterIndex, InputStream inputStream, long length) {

  }

  @Override
  public void setNClob(int parameterIndex, Reader reader, long length) {

  }

  @Override
  public void setSQLXML(int parameterIndex, SQLXML xmlObject) {

  }

  @Override
  public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) {

  }

  @Override
  public void setAsciiStream(int parameterIndex, InputStream x, long length) {

  }

  @Override
  public void setBinaryStream(int parameterIndex, InputStream x, long length) {

  }

  @Override
  public void setCharacterStream(int parameterIndex, Reader reader, long length) {

  }

  @Override
  public void setAsciiStream(int parameterIndex, InputStream x) {

  }

  @Override
  public void setBinaryStream(int parameterIndex, InputStream x) {

  }

  @Override
  public void setCharacterStream(int parameterIndex, Reader reader) {

  }

  @Override
  public void setNCharacterStream(int parameterIndex, Reader value) {

  }

  @Override
  public void setClob(int parameterIndex, Reader reader) {

  }

  @Override
  public void setBlob(int parameterIndex, InputStream inputStream) {

  }

  @Override
  public void setNClob(int parameterIndex, Reader reader) {

  }

  @Override
  public ResultSet executeQuery(String sql) {
    openCurrentFile(sql);
    currentResultSet = new MockResultSet();
    return currentResultSet;
  }

  @Override
  public int executeUpdate(String sql) {
    return 0;
  }

  @Override
  public void close() {
    isClosed.set(true);
  }

  @Override
  public int getMaxFieldSize() {
    return 0;
  }

  @Override
  public void setMaxFieldSize(int max) {

  }

  @Override
  public int getMaxRows() {
    return 0;
  }

  @Override
  public void setMaxRows(int max) {
  }

  @Override
  public void setEscapeProcessing(boolean enable) {

  }

  @Override
  public int getQueryTimeout() {
    return 1000;
  }

  @Override
  public void setQueryTimeout(int seconds) {
  }

  @Override
  public void cancel() {
  }

  @Override
  public SQLWarning getWarnings() {
    return warnings;
  }

  @Override
  public void clearWarnings() {
    warnings = null;
  }

  @Override
  public void setCursorName(String name) {
    throw new UnsupportedOperationException("setCursorName");
  }

  @Override
  public boolean execute(String sql) {
    openCurrentFile(sql);
    return execute();
  }

  @Override
  public ResultSet getResultSet() {
    return currentResultSet;
  }

  @Override
  public int getUpdateCount() {
    return -1;
  }

  @Override
  public boolean getMoreResults() {
    return false;
  }

  @Override
  public int getFetchDirection() {
    return ResultSet.FETCH_FORWARD;
  }

  @Override
  public void setFetchDirection(int direction) {
  }

  @Override
  public int getFetchSize() {
    return 0;
  }

  @Override
  public void setFetchSize(int rows) {
  }

  @Override
  public int getResultSetConcurrency() {
    throw new UnsupportedOperationException("getResultSetConcurrency");
  }

  @Override
  public int getResultSetType() {
    throw new UnsupportedOperationException("getResultSetType");
  }

  @Override
  public void addBatch(String sql) {

  }

  @Override
  public void clearBatch() {

  }

  @Override
  public int[] executeBatch() {
    throw new UnsupportedOperationException("executeBatch");
  }

  @Override
  public Connection getConnection() {
    throw new UnsupportedOperationException("getConnection");
  }

  @Override
  public boolean getMoreResults(int current) {
    return false;
  }

  @Override
  public ResultSet getGeneratedKeys() {
    return null;
  }

  @Override
  public int executeUpdate(String sql, int autoGeneratedKeys) {
    return 0;
  }

  @Override
  public int executeUpdate(String sql, int[] columnIndexes) {
    return 0;
  }

  @Override
  public int executeUpdate(String sql, String[] columnNames) {
    return 0;
  }

  @Override
  public boolean execute(String sql, int autoGeneratedKeys) {
    return execute(sql);
  }

  @Override
  public boolean execute(String sql, int[] columnIndexes) {
    return execute(sql);
  }

  @Override
  public boolean execute(String sql, String[] columnNames) {
    return execute(sql);
  }

  @Override
  public int getResultSetHoldability() {
    return 2;
  }

  @Override
  public boolean isClosed() {
    return isClosed.get();
  }

  @Override
  public boolean isPoolable() {
    return false;
  }

  @Override
  public void setPoolable(boolean poolable) {
  }

  @Override
  public void closeOnCompletion() {
  }

  @Override
  public boolean isCloseOnCompletion() {
    return false;
  }

  @Override
  public <T> T unwrap(Class<T> iface) {
    throw new UnsupportedOperationException("unwrap");
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) {
    return false;
  }
}
