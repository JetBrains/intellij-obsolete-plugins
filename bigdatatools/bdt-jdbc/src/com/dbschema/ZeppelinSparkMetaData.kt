// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.dbschema

import no.maddin.mockjdbc.MockResultSet
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.ResultSet
import java.sql.RowIdLifetime
import java.sql.SQLException
import java.sql.SQLFeatureNotSupportedException

class ZeppelinSparkMetaData internal constructor(
  private val connection: ZeppelinSparkConnection,
  private val driver: ZeppelinSparkJdbcDriver
) : DatabaseMetaData {
  override fun getSchemas(): ResultSet? = null

  override fun getCatalogs(): ResultSet? = null

  override fun getTables(
    catalogName: String, schemaPattern: String,
    tableNamePattern: String, types: Array<String>
  ): ResultSet? = null

  override fun getColumns(
    catalogName: String, schemaName: String,
    tableNamePattern: String, columnNamePattern: String
  ): ResultSet = MockResultSet()

  override fun getPrimaryKeys(
    catalogName: String,
    schemaName: String,
    tableNamePattern: String
  ): ResultSet? = null

  override fun getIndexInfo(
    catalogName: String, schemaName: String, tableNamePattern: String, unique: Boolean,
    approximate: Boolean
  ): ResultSet? = null

  override fun getTypeInfo(): ResultSet? = null

  @Throws(SQLException::class)
  override fun <T> unwrap(iface: Class<T>): T = throw SQLFeatureNotSupportedException()

  @Throws(SQLException::class)
  override fun isWrapperFor(iface: Class<*>?): Boolean = throw SQLFeatureNotSupportedException()

  @Throws(SQLException::class)
  override fun allProceduresAreCallable(): Boolean = throw SQLFeatureNotSupportedException()

  @Throws(SQLException::class)
  override fun allTablesAreSelectable(): Boolean = throw SQLFeatureNotSupportedException()

  override fun getURL(): String? = null

  override fun getUserName(): String? = null

  override fun isReadOnly(): Boolean = throw SQLFeatureNotSupportedException()

  override fun nullsAreSortedHigh(): Boolean = throw SQLFeatureNotSupportedException()

  @Throws(SQLException::class)
  override fun nullsAreSortedLow(): Boolean = throw SQLFeatureNotSupportedException()

  @Throws(SQLException::class)
  override fun nullsAreSortedAtStart(): Boolean = throw SQLFeatureNotSupportedException()

  override fun nullsAreSortedAtEnd(): Boolean = throw SQLFeatureNotSupportedException()

  override fun getDatabaseProductName(): String = "Zeppelin Spark SQL"

  override fun getDatabaseProductVersion(): String = "0.0.0"

  override fun getDriverName(): String = "Zeppelin Spark JDBC Driver"

  override fun getDriverVersion(): String = driver.version

  override fun getDriverMajorVersion(): Int = driver.majorVersion

  override fun getDriverMinorVersion(): Int = driver.minorVersion

  override fun usesLocalFiles(): Boolean = throw SQLFeatureNotSupportedException()

  override fun usesLocalFilePerTable(): Boolean = throw SQLFeatureNotSupportedException()

  override fun supportsMixedCaseIdentifiers(): Boolean = false

  override fun storesUpperCaseIdentifiers(): Boolean = false

  override fun storesLowerCaseIdentifiers(): Boolean = true

  override fun storesMixedCaseIdentifiers(): Boolean = false

  override fun supportsMixedCaseQuotedIdentifiers(): Boolean = true

  override fun storesUpperCaseQuotedIdentifiers(): Boolean = true

  override fun storesLowerCaseQuotedIdentifiers(): Boolean = true

  override fun storesMixedCaseQuotedIdentifiers(): Boolean = true

  override fun getIdentifierQuoteString(): String = "\""

  override fun getSQLKeywords(): String? = null

  override fun getNumericFunctions(): String? = null

  override fun getStringFunctions(): String? = null

  override fun getSystemFunctions(): String? = null

  override fun getTimeDateFunctions(): String? = null

  override fun getSearchStringEscape(): String? = null

  override fun getExtraNameCharacters(): String = ""

  override fun supportsAlterTableWithAddColumn(): Boolean = false

  override fun supportsAlterTableWithDropColumn(): Boolean = false

  override fun supportsColumnAliasing(): Boolean = false

  override fun nullPlusNonNullIsNull(): Boolean = false

  override fun supportsConvert(): Boolean = false

  override fun supportsConvert(fromType: Int, toType: Int): Boolean = false

  override fun supportsTableCorrelationNames(): Boolean = false

  override fun supportsDifferentTableCorrelationNames(): Boolean = false

  override fun supportsExpressionsInOrderBy(): Boolean = false

  override fun supportsOrderByUnrelated(): Boolean = false

  override fun supportsGroupBy(): Boolean = false

  override fun supportsGroupByUnrelated(): Boolean = false

  override fun supportsGroupByBeyondSelect(): Boolean = false

  override fun supportsLikeEscapeClause(): Boolean = false

  override fun supportsMultipleResultSets(): Boolean = false

  override fun supportsMultipleTransactions(): Boolean = false

  override fun supportsNonNullableColumns(): Boolean = true

  override fun supportsMinimumSQLGrammar(): Boolean = false

  override fun supportsCoreSQLGrammar(): Boolean = false

  override fun supportsExtendedSQLGrammar(): Boolean = false

  override fun supportsANSI92EntryLevelSQL(): Boolean = false

  override fun supportsANSI92IntermediateSQL(): Boolean = false

  override fun supportsANSI92FullSQL(): Boolean = false

  override fun supportsIntegrityEnhancementFacility(): Boolean = false

  override fun supportsOuterJoins(): Boolean = false

  override fun supportsFullOuterJoins(): Boolean = false

  @Throws(SQLException::class)
  override fun supportsLimitedOuterJoins(): Boolean = false

  @Throws(SQLException::class)
  override fun getSchemaTerm(): String {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun getProcedureTerm(): String {
    throw SQLFeatureNotSupportedException()
  }

  override fun getCatalogTerm(): String = "keyspace"

  @Throws(SQLException::class)
  override fun isCatalogAtStart(): Boolean = false

  override fun getCatalogSeparator(): String = "."

  @Throws(SQLException::class)
  override fun supportsSchemasInDataManipulation(): Boolean = false

  @Throws(SQLException::class)
  override fun supportsSchemasInProcedureCalls(): Boolean = false

  @Throws(SQLException::class)
  override fun supportsSchemasInTableDefinitions(): Boolean = false

  @Throws(SQLException::class)
  override fun supportsSchemasInIndexDefinitions(): Boolean = false

  @Throws(SQLException::class)
  override fun supportsSchemasInPrivilegeDefinitions(): Boolean = false

  override fun supportsCatalogsInDataManipulation(): Boolean = true

  @Throws(SQLException::class)
  override fun supportsCatalogsInProcedureCalls(): Boolean = false

  @Throws(SQLException::class)
  override fun supportsCatalogsInTableDefinitions(): Boolean = false

  @Throws(SQLException::class)
  override fun supportsCatalogsInIndexDefinitions(): Boolean = false

  @Throws(SQLException::class)
  override fun supportsCatalogsInPrivilegeDefinitions(): Boolean = false

  @Throws(SQLException::class)
  override fun supportsPositionedDelete(): Boolean = false

  @Throws(SQLException::class)
  override fun supportsPositionedUpdate(): Boolean = false

  @Throws(SQLException::class)
  override fun supportsSelectForUpdate(): Boolean = false

  @Throws(SQLException::class)
  override fun supportsStoredProcedures(): Boolean = false

  @Throws(SQLException::class)
  override fun supportsSubqueriesInComparisons(): Boolean = false

  @Throws(SQLException::class)
  override fun supportsSubqueriesInExists(): Boolean = false

  @Throws(SQLException::class)
  override fun supportsSubqueriesInIns(): Boolean = false

  @Throws(SQLException::class)
  override fun supportsSubqueriesInQuantifieds(): Boolean = false

  @Throws(SQLException::class)
  override fun supportsCorrelatedSubqueries(): Boolean = false

  @Throws(SQLException::class)
  override fun supportsUnion(): Boolean = false

  @Throws(SQLException::class)
  override fun supportsUnionAll(): Boolean = false

  @Throws(SQLException::class)
  override fun supportsOpenCursorsAcrossCommit(): Boolean = false

  @Throws(SQLException::class)
  override fun supportsOpenCursorsAcrossRollback(): Boolean = false

  @Throws(SQLException::class)
  override fun supportsOpenStatementsAcrossCommit(): Boolean = false

  @Throws(SQLException::class)
  override fun supportsOpenStatementsAcrossRollback(): Boolean = false

  @Throws(SQLException::class)
  override fun getMaxBinaryLiteralLength(): Int {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun getMaxCharLiteralLength(): Int {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun getMaxColumnNameLength(): Int {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun getMaxColumnsInGroupBy(): Int {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun getMaxColumnsInIndex(): Int {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun getMaxColumnsInOrderBy(): Int {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun getMaxColumnsInSelect(): Int {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun getMaxColumnsInTable(): Int {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun getMaxConnections(): Int {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun getMaxCursorNameLength(): Int {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun getMaxIndexLength(): Int {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun getMaxSchemaNameLength(): Int {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun getMaxProcedureNameLength(): Int {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun getMaxCatalogNameLength(): Int {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun getMaxRowSize(): Int {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun doesMaxRowSizeIncludeBlobs(): Boolean {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun getMaxStatementLength(): Int {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun getMaxStatements(): Int {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun getMaxTableNameLength(): Int {
    throw SQLFeatureNotSupportedException()
  }

  override fun getMaxTablesInSelect(): Int {
    return 1
  }

  @Throws(SQLException::class)
  override fun getMaxUserNameLength(): Int {
    throw SQLFeatureNotSupportedException()
  }

  override fun getDefaultTransactionIsolation(): Int {
    return Connection.TRANSACTION_NONE
  }

  /**
   * Cassandra doesn't support transactions, but document updates are atomic.
   */
  override fun supportsTransactions(): Boolean {
    return false
  }

  @Throws(SQLException::class)
  override fun supportsTransactionIsolationLevel(level: Int): Boolean {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun supportsDataDefinitionAndDataManipulationTransactions(): Boolean {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun supportsDataManipulationTransactionsOnly(): Boolean {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun dataDefinitionCausesTransactionCommit(): Boolean {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun dataDefinitionIgnoredInTransactions(): Boolean {
    throw SQLFeatureNotSupportedException()
  }

  override fun getProcedures(
    catalogName: String, schemaPattern: String,
    procedureNamePattern: String
  ): ResultSet? = null

  override fun getProcedureColumns(
    catalogName: String, schemaPattern: String, procedureNamePattern: String,
    columnNamePattern: String
  ): ResultSet? = null

  override fun getTableTypes(): ResultSet? = null

  override fun getColumnPrivileges(
    catalogName: String, schemaName: String,
    table: String, columnNamePattern: String
  ): ResultSet {
    return MockResultSet()
  }

  override fun getTablePrivileges(
    catalogName: String,
    schemaPattern: String,
    tableNamePattern: String
  ): ResultSet? {
    return null
  }

  override fun getBestRowIdentifier(
    catalogName: String, schemaName: String, table: String, scope: Int,
    nullable: Boolean
  ): ResultSet? {
    return null
  }

  override fun getVersionColumns(
    catalogName: String,
    schemaName: String,
    table: String
  ): ResultSet? {
    return null
  }

  override fun getExportedKeys(
    catalogName: String,
    schemaName: String,
    tableNamePattern: String
  ): ResultSet? {
    return null
  }

  override fun getImportedKeys(
    catalogName: String,
    schemaName: String,
    tableNamePattern: String
  ): ResultSet? {
    return null
  }

  override fun getCrossReference(
    parentCatalog: String, parentSchema: String, parentTable: String,
    foreignCatalog: String, foreignSchema: String, foreignTable: String
  ): ResultSet? {
    return null
  }

  override fun supportsResultSetType(type: Int): Boolean = type == ResultSet.TYPE_FORWARD_ONLY

  @Throws(SQLException::class)
  override fun supportsResultSetConcurrency(type: Int, concurrency: Int): Boolean {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun ownUpdatesAreVisible(type: Int): Boolean {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun ownDeletesAreVisible(type: Int): Boolean {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun ownInsertsAreVisible(type: Int): Boolean {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun othersUpdatesAreVisible(type: Int): Boolean {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun othersDeletesAreVisible(type: Int): Boolean {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun othersInsertsAreVisible(type: Int): Boolean {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun updatesAreDetected(type: Int): Boolean {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun deletesAreDetected(type: Int): Boolean {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun insertsAreDetected(type: Int): Boolean {
    throw SQLFeatureNotSupportedException()
  }

  override fun supportsBatchUpdates(): Boolean = true

  override fun getUDTs(
    catalogName: String,
    schemaPattern: String,
    typeNamePattern: String,
    types: IntArray
  ): ResultSet? = null

  override fun getConnection(): Connection = connection

  override fun supportsSavepoints(): Boolean = false

  @Throws(SQLException::class)
  override fun supportsNamedParameters(): Boolean {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun supportsMultipleOpenResults(): Boolean {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun supportsGetGeneratedKeys(): Boolean {
    throw SQLFeatureNotSupportedException()
  }

  override fun getSuperTypes(
    catalogName: String,
    schemaPattern: String,
    typeNamePattern: String
  ): ResultSet? {
    return null
  }

  override fun getSuperTables(
    catalogName: String,
    schemaPattern: String,
    tableNamePattern: String
  ): ResultSet? {
    return null
  }

  override fun getAttributes(
    catalogName: String, schemaPattern: String, typeNamePattern: String,
    attributeNamePattern: String
  ): ResultSet? {
    return null
  }

  @Throws(SQLException::class)
  override fun supportsResultSetHoldability(holdability: Int): Boolean {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun getResultSetHoldability(): Int {
    throw SQLFeatureNotSupportedException()
  }

  override fun getDatabaseMajorVersion(): Int {
    return databaseProductVersion.split("\\.".toRegex()).toTypedArray()[0].toInt()
  }

  override fun getDatabaseMinorVersion(): Int {
    return databaseProductVersion.split("\\.".toRegex()).toTypedArray()[1].toInt()
  }

  override fun getJDBCMajorVersion(): Int {
    return 4
  }

  override fun getJDBCMinorVersion(): Int {
    return 2
  }

  override fun getSQLStateType(): Int {
    return DatabaseMetaData.sqlStateXOpen
  }

  @Throws(SQLException::class)
  override fun locatorsUpdateCopy(): Boolean {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun supportsStatementPooling(): Boolean {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun getRowIdLifetime(): RowIdLifetime {
    throw SQLFeatureNotSupportedException()
  }

  override fun getSchemas(catalogName: String, schemaPattern: String): ResultSet? = null

  @Throws(SQLException::class)
  override fun supportsStoredFunctionsUsingCallSyntax(): Boolean {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun autoCommitFailureClosesAllResultSets(): Boolean {
    throw SQLFeatureNotSupportedException()
  }

  @Throws(SQLException::class)
  override fun getClientInfoProperties(): ResultSet {
    throw SQLFeatureNotSupportedException()
  }

  override fun getFunctions(
    catalogName: String,
    schemaPattern: String,
    functionNamePattern: String
  ): ResultSet? = null

  override fun getFunctionColumns(
    catalogName: String, schemaPattern: String, functionNamePattern: String,
    columnNamePattern: String
  ): ResultSet? = null

  override fun getPseudoColumns(
    catalogName: String,
    schemaPattern: String,
    tableNamePattern: String,
    columnNamePattern: String
  ): ResultSet? = null

  @Throws(SQLException::class)
  override fun generatedKeyAlwaysReturned(): Boolean {
    throw SQLFeatureNotSupportedException()
  }

}