package com.jetbrains.bigdatatools.glue.client

import com.intellij.bigdatatools.awsBase.connection.AwsConnectionUtils
import com.intellij.bigdatatools.awsBase.connection.auth.AuthenticationType
import com.intellij.bigdatatools.awsBase.connection.auth.AwsAuthUtil
import com.intellij.bigdatatools.awsBase.driver.AwsCredentialController
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.connection.MonitoringClient
import com.jetbrains.bigdatatools.glue.monitoring.models.GlueResourceShareType
import com.jetbrains.bigdatatools.glue.settings.GlueConnectionData
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.profiles.ProfileFile
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.glue.GlueClient
import software.amazon.awssdk.services.glue.model.Database
import software.amazon.awssdk.services.glue.model.GetDatabasesRequest
import software.amazon.awssdk.services.glue.model.GetPartitionsRequest
import software.amazon.awssdk.services.glue.model.GetTablesRequest
import software.amazon.awssdk.services.glue.model.Partition
import software.amazon.awssdk.services.glue.model.Table

class BdtGlueClient(project: Project?,
                    val connData: GlueConnectionData) : MonitoringClient(project) {
  private val authentication = AwsAuthUtil.getPrimaryAuthentication(connData.getAwsInfo())

  private val credentialsController = AwsCredentialController(authentication.getCredentialsProvider())

  private var client: GlueClient? = null


  override fun getRealUri(): String = "https://console.aws.amazon.com/elasticmapreduce/home?region=${connData.region}"

  override fun dispose() {
    client?.close()
  }

  override fun connectInner(calledByUser: Boolean) {
    credentialsController.wrapWithAllowDialogs(calledByUser) {
      if (client == null || calledByUser) {
        client?.close()
        client = createClient()
      }

      if (calledByUser)
        getDatabases(1)
    }
  }

  override fun checkConnectionInner() {
    getDatabases(1)
  }


  fun getDatabases(limit: Int? = null, resourceShareType: GlueResourceShareType? = null): List<Database> {
    val request = GetDatabasesRequest.builder().maxResults(limit).resourceShareType(resourceShareType?.awsType)
    return client?.getDatabases(request.build())?.databaseList() ?: listOf()
  }

  fun getTables(catalog: String?, database: String, expression: String?, limit: Int?): List<Table> {
    val request = GetTablesRequest.builder().catalogId(catalog).databaseName(database).expression(expression).maxResults(limit).build()
    return client?.getTables(request)?.tableList() ?: emptyList()
  }

  fun getSchemaPartitions(catalog: String, database: String?, table: String?, maxParts: Int? = null): List<Partition> {
    val request = GetPartitionsRequest.builder()
      .catalogId(catalog)
      .databaseName(database)
      .tableName(table)
      .maxResults(maxParts)
      .excludeColumnSchema(true)
      .build()
    return client?.getPartitions(request)?.partitions() ?: emptyList()
  }


  private fun createClient(): GlueClient? {
    val httpClient = AwsConnectionUtils.createHttpClient(connData.getProxy(), connData.trustAllSsl == true)
    val overrideConfiguration = if (connData.activeAuthenticationType in setOf(AuthenticationType.KEY_PAIR.id, AuthenticationType.ANON.id,
                                                                               AuthenticationType.PROFILE_FROM_CREDENTIALS_FILE.id)) {
      val clientConfiguration = ClientOverrideConfiguration.builder()
      clientConfiguration.defaultProfileFile(ProfileFile.aggregator().build()).build()
    }
    else {
      null
    }

    val clientBuilder = GlueClient.builder()
      .credentialsProvider(credentialsController.credentials)
      .region(Region.of(connData.region))
      .httpClient(httpClient)
    if (overrideConfiguration != null)
      clientBuilder.overrideConfiguration(overrideConfiguration)
    return clientBuilder.build()
  }
}

