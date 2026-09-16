package com.jetbrains.bigdatatools.hivemetastore.client

import com.intellij.bigdatatools.coreUi.connection.exception.BdtConfigurationException
import com.intellij.bigdatatools.coreUi.settings.components.BdtPropertyComponent
import com.intellij.bigdatatools.coreUi.util.BdtUrlUtils
import com.intellij.bigdatatools.coreUi.util.withPluginClassLoader
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.connection.tunnel.BdtSshTunnelService
import com.jetbrains.bigdatatools.common.connection.tunnel.model.ConnectionSshTunnelInfo
import com.jetbrains.bigdatatools.common.monitoring.connection.MonitoringClient
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.settings.kerberos.BdtJaasConfig
import com.jetbrains.bigdatatools.common.settings.kerberos.BdtKerberosManager
import com.jetbrains.bigdatatools.common.settings.kerberos.KerberosCacheCallBackHandler
import com.jetbrains.bigdatatools.hivemetastore.lib.HiveMetaStoreClient
import com.jetbrains.bigdatatools.hivemetastore.rfs.HiveMetastoreDriver
import com.jetbrains.bigdatatools.hivemetastore.settings.HiveMetastoreConnectionData
import com.jetbrains.bigdatatools.hivemetastore.settings.HiveMetastorePropertySource
import com.jetbrains.bigdatatools.hivemetastore.utils.HiveMessagesBundle
import com.jetbrains.bigdatatools.hivemetastore.utils.HiveMetastoreUtils.catalog
import com.jetbrains.bigdatatools.hivemetastore.utils.HiveMetastoreUtils.database
import com.jetbrains.bigdatatools.hivemetastore.utils.HiveMetastoreUtils.table
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hive.metastore.TableType
import org.apache.hadoop.hive.metastore.api.Catalog
import org.apache.hadoop.hive.metastore.api.Database
import org.apache.hadoop.hive.metastore.api.FieldSchema
import org.apache.hadoop.hive.metastore.api.Partition
import org.apache.hadoop.hive.metastore.api.Table
import org.apache.hadoop.hive.metastore.conf.MetastoreConf
import org.apache.hadoop.hive.metastore.conf.MetastoreConf.ConfVars
import org.apache.hadoop.security.UserGroupInformation
import org.com.jetbrains.bigdatatools.hdfs.settings.HdfsJavaSettingsCustomizer
import java.io.File
import javax.security.auth.Subject
import javax.security.auth.login.LoginContext

@Suppress("TestOnlyProblems")
class HiveClient(project: Project?,
                 val connectionData: HiveMetastoreConnectionData,
                 private val testConnection: Boolean) : MonitoringClient(project) {

  inner class InnerClient(metastoreConf: Configuration, private val tunnelsDisposable: Disposable) : Disposable {
    val client: Result<HiveMetaStoreClient> = runCatching {
      val isKerberosEnabled = metastoreConf[HdfsJavaSettingsCustomizer.HADOOP_AUTH_CONF] == HdfsJavaSettingsCustomizer.HADOOP_AUTH_KERBEROS_KEY
      if (isKerberosEnabled) {
        setupKerberosAuth(metastoreConf)
      }

      val hiveMetaStoreClient = HiveMetaStoreClient(metastoreConf)
      try {
        hiveMetaStoreClient.catalogs
        hiveMetaStoreClient.isV3Connection = true
      }
      catch (t: Throwable) {
        if (t.message == "Invalid method name: 'get_catalogs'") {
          hiveMetaStoreClient.isV3Connection = false
        }
        else {
          throw t
        }
      }
      hiveMetaStoreClient
    }

    init {
      Disposer.register(this, tunnelsDisposable)
    }

    override fun dispose() {
      client.onSuccess {
        it.close()
        Disposer.dispose(tunnelsDisposable)
      }
    }

    private fun setupKerberosAuth(metastoreConf: Configuration) {
      BdtKerberosManager.instance.setupKerberosValues()

      if (connectionData.useKerberosTicketCache) {
        BdtKerberosManager.instance.validateCacheSupported()
        synchronized(syncObj) {
          withPluginClassLoader {
            UserGroupInformation.setConfiguration(metastoreConf)
            val jaasConfigParams = "com.sun.security.auth.module.Krb5LoginModule required client=TRUE useTicketCache=true;"
            val lc: LoginContext = object : LoginContext("jaasEntryName", Subject(),
                                                         KerberosCacheCallBackHandler(),
                                                         BdtJaasConfig(jaasConfigParams)) {}
            lc.login()
            UserGroupInformation.loginUserFromSubject(lc.subject)
          }
        }
      }
      else {
        val principal = metastoreConf[ConfVars.KERBEROS_PRINCIPAL.hiveName]
        val keytab = metastoreConf[ConfVars.KERBEROS_KEYTAB_FILE.hiveName]
        BdtKerberosManager.instance.validatePrincipal(principal)
        BdtKerberosManager.instance.validateKeytab(keytab)

        synchronized(syncObj) {
          withPluginClassLoader {
            UserGroupInformation.setConfiguration(metastoreConf)
            UserGroupInformation.loginUserFromKeytab(principal, keytab)

          }
        }
      }
    }
  }

  private var innerClient: InnerClient? = null

  private val client: HiveMetaStoreClient
    get() = innerClient?.client?.getOrThrow() ?: error(HiveMessagesBundle.message("error.is.not.inited"))

  override fun getRealUri() = connectionData.uri

  override fun dispose() {
    synchronized(this) {
      innerClient?.let { Disposer.dispose(it) }
    }
  }

  override fun connectInner(calledByUser: Boolean) {
    synchronized(this) {
      innerClient?.let { Disposer.dispose(it) }
      val innerDisposable = Disposer.newDisposable()
      val metastoreConf = calculateMetastoreConf(innerDisposable)

      val createdClient = InnerClient(metastoreConf, innerDisposable)
      innerClient = createdClient
      createdClient.client.getOrThrow()
    }
  }

  override fun checkConnectionInner() = synchronized(this) {
    if (!client.tTransport.isOpen) {
      error("Transport is closed to ${client.getMetaConf(MetastoreConf.ConfVars.THRIFT_URIS.varname)}")
    }
  }

  fun getCatalogInfo(rfsPath: RfsPath) = rfsPath.catalog?.let { getCatalog(it) }

  private fun getCatalog(catalog: String): Catalog? = blockWithRestore {
    client.getCatalog(catalog)
  }

  fun getDatabaseInfo(rfsPath: RfsPath): Database? = rfsPath.database?.let {
    getDatabaseInfo(rfsPath.catalog ?: "", rfsPath.database ?: "")
  }

  fun getDatabaseInfo(database: String): Database? = blockWithRestore {
    client.getDatabase(database)
  }


  private fun getDatabaseInfo(catalog: String, database: String): Database? = blockWithRestore {
    client.getDatabase(catalog, database)
  }


  fun getTables(catalog: String?, database: String, tablePattern: String?, type: TableType? = null): List<String> = blockWithRestore {
    type?.let { client.getTables(catalog, database, tablePattern, type) }
    ?: client.getTables(catalog, database, tablePattern)
    ?: emptyList()
  }

  fun getTableInfo(rfsPath: RfsPath) = blockWithRestore {
    rfsPath.table?.let {
      client.getTable(rfsPath.catalog, rfsPath.database, rfsPath.table)
    }
  }

  fun getTable(catalog: String?, database: String, table: String): Table? = blockWithRestore {
    client.getTable(catalog, database, table)

  }


  fun getSchema(catalog: String?, database: String?, table: String?): List<FieldSchema>? = blockWithRestore {
    client.getSchema(catalog, database, table)
  }

  fun getSchema(database: String?, table: String?): List<FieldSchema>? = blockWithRestore {
    client.getSchema(database, table)
  }

  fun getPartitionsNames(catalog: String, database: String, table: String, maxParts: Int? = null): List<String> = blockWithRestore {
    client.listPartitionNames(catalog, database, table, maxParts ?: 1000) ?: emptyList()
  }

  fun getPartitions(catalog: String, database: String?, table: String?, maxParts: Int? = null): List<Partition> = blockWithRestore {
    client.listPartitions(catalog, database, table, maxParts ?: 1000) ?: emptyList()
  }


  fun getCatalogs(): List<String> = blockWithRestore {
    client.catalogs ?: emptyList()
  }


  fun getDatabases(catalog: String?, databasePattern: String?): List<String> = blockWithRestore {
    client.getDatabases(catalog, databasePattern?.ifBlank { "*" } ?: "*") ?: emptyList()
  }

  private fun <T> blockWithRestore(body: () -> T): T {
    synchronized(this) {
      if (!client.tTransport.isOpen)
        client.reconnect()
      return body()
    }
  }

  private fun calculateMetastoreConf(innerDisposable: Disposable): Configuration {
    val metastoreConf = run {
      when (connectionData.propertySource) {
        HiveMetastorePropertySource.DIRECT -> getDirectConfig()
        HiveMetastorePropertySource.FILE -> getFolderConfig()
      }
    }

    updateUris(metastoreConf)
    setupSsh(metastoreConf, innerDisposable)
    return metastoreConf
  }

  private fun setupSsh(metastoreConf: Configuration, innerDisposable: Disposable) {
    if (connectionData.getTunnelData().isEnabled) {
      val originUris = metastoreConf.get(ConfVars.THRIFT_URIS.varname)
      val tunneledUris = createTunnelUris(originUris, innerDisposable)
      metastoreConf.set(ConfVars.THRIFT_URIS.varname, tunneledUris)
    }
  }

  private fun updateUris(metastoreConf: Configuration) {
    val uris = metastoreConf.get(ConfVars.THRIFT_URIS.varname)?.ifBlank { null } ?: metastoreConf.get(
      ConfVars.THRIFT_URIS.hiveName)?.ifBlank { null }
    if (uris == null) {
      throw BdtConfigurationException(HiveMessagesBundle.message("error.uris.not.found"))
    }
    metastoreConf.set(ConfVars.THRIFT_URIS.varname, uris)
    metastoreConf.unset(ConfVars.THRIFT_URIS.hiveName)
  }

  private fun getDirectConfig(): Configuration {
    val metastoreConf = createBaseConfiguration()

    val properties = BdtPropertyComponent.parseProperties(connectionData.properties)
    properties.forEach {
      metastoreConf.set(it.name, it.value)
    }
    return metastoreConf
  }


  private fun getFolderConfig(): Configuration {
    if (connectionData.configFolderPath?.isNotBlank() == true) {
      System.setProperty(HiveMetastoreDriver.TEST_ENV_WORKAROUND + "METASTORE_CONF_DIR", connectionData.configFolderPath?.trim() ?: "")
    }

    val metastoreConf = try {
      newMetastoreConf(File(connectionData.configFolderPath!!))
    }
    catch (t: Throwable) {
      throw BdtConfigurationException(HiveMessagesBundle.message("error.cannot.load.config.from.folder"), t)
    }

    metastoreConf.set(MetastoreConf.ConfVars.THRIFT_URI_SELECTION.varname, "SEQUENTIAL")
    return metastoreConf
  }

  @Suppress("DEPRECATION")
  private fun newMetastoreConf(file: File): Configuration {
    val conf = createBaseConfiguration()

    File(file, "hive-site.xml").takeIf { it.exists() }?.toURL()?.let { conf.addResource(it) }
    File(file, "hivemetastore-site.xml").takeIf { it.exists() }?.toURL()?.let { conf.addResource(it) }
    File(file, "metastore-site.xml").takeIf { it.exists() }?.toURL()?.let { conf.addResource(it) }

    // If a system property that matches one of our conf value names is set then use the value
    // it's set to set our own conf value.
    for (`var` in ConfVars.entries) {
      if (System.getProperty(`var`.varname) != null) {
        logger.debug("Setting conf value " + `var`.varname + " using value " +
                     System.getProperty(`var`.varname))
        conf[`var`.varname] = System.getProperty(`var`.varname)
      }
    }

    // Pick up any system properties that start with "hive." and set them in our config.
    // This way we can properly pull any Hive values from the environment without needing to know all
    // the Hive config values.
    System.getProperties().stringPropertyNames()
      .filter { s: String -> s.startsWith("hive.") }
      .forEach { s: String ->
        val v = System.getProperty(s)
        logger.debug("Picking up system property $s with value $v")
        conf[s] = v
      }

    // If we are going to validate the schema, make sure we don't create it
    if (MetastoreConf.getBoolVar(conf, ConfVars.SCHEMA_VERIFICATION)) {
      MetastoreConf.setBoolVar(conf, ConfVars.AUTO_CREATE_ALL, false)
    }
    return conf
  }

  private fun createTunnelUris(originUris: String, innerDisposable: Disposable): String {
    val listUris = originUris.split(",").map { it.trim() }.filter { it.isNotBlank() }
    val tunneledUris = listUris.map { createTunnelForUrl(it, innerDisposable) }
    return tunneledUris.joinToString()
  }

  private fun createTunnelForUrl(originUrl: String, innerDisposable: Disposable): String {
    val url = BdtUrlUtils.convertToUrlObject(originUrl.removePrefix(HiveMetastoreDriver.THRIFT_PREFIX))
    val tunnel = ConnectionSshTunnelInfo(connectionData.getTunnelData().configId, url.host, url.port, project).takeIf { connectionData.getTunnelData().isEnabled }
    val tunnelHandler = requireNotNull(
      BdtSshTunnelService.createIfRequiredInternal(project, tunnel, connectionData.innerId, testConnection))
    Disposer.register(innerDisposable, tunnelHandler)
    return "${HiveMetastoreDriver.THRIFT_PREFIX}localhost:${tunnelHandler.localPort}"
  }

  private fun createBaseConfiguration(): Configuration {
    val metastoreConf = Configuration()
    metastoreConf.set(ConfVars.THRIFT_URIS.varname, connectionData.uri)
    metastoreConf.set(ConfVars.THRIFT_URI_SELECTION.varname, "SEQUENTIAL")
    metastoreConf.set(ConfVars.EXECUTE_SET_UGI.varname, "false")
    metastoreConf.set(ConfVars.CLIENT_SOCKET_TIMEOUT.varname, 1000.toString())
    return metastoreConf
  }

  companion object {
    private val syncObj = Any()
    private val logger = Logger.getInstance(this::class.java)
  }
}

