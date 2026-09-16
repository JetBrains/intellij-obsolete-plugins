// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.dbschema

import java.sql.Connection
import java.sql.Driver
import java.sql.DriverManager
import java.sql.DriverPropertyInfo
import java.sql.SQLException
import java.sql.SQLFeatureNotSupportedException
import java.util.Properties
import java.util.logging.Logger

/**
 * Mock implementation of Zeppelin Spark JDBC Driver for IDEA Big Data Tools plugin .
 * Can accept any URLS and return stubs for all operations.
 */
class ZeppelinSparkJdbcDriver : Driver {
  companion object {
    init {
      try {
        DriverManager.registerDriver(ZeppelinSparkJdbcDriver())
      }
      catch (ex: SQLException) {
        //
      }
    }
  }

  override fun connect(url: String, info: Properties): Connection = ZeppelinSparkConnection(this)
  override fun acceptsURL(url: String): Boolean = true
  override fun getPropertyInfo(url: String, info: Properties): Array<DriverPropertyInfo>? = null

  val version: String = "0.2.0"
  override fun getMajorVersion(): Int = 0
  override fun getMinorVersion(): Int = 1
  override fun jdbcCompliant(): Boolean = true
  override fun getParentLogger(): Logger = throw SQLFeatureNotSupportedException()
}