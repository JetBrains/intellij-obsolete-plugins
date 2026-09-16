package com.jetbrains.bigdatatools.glue.settings

import com.intellij.bigdatatools.awsBase.connection.auth.AuthenticationType
import com.intellij.bigdatatools.aws.driver.AwsCompatibleConnectionData
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionGroup
import com.intellij.bigdatatools.glue.icons.BigdatatoolsGlueIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.jetbrains.bigdatatools.common.connection.tunnel.model.ConnectionSshTunnelData
import com.jetbrains.bigdatatools.common.connection.tunnel.model.ConnectionSshTunnelDataLegacy
import com.jetbrains.bigdatatools.common.connection.tunnel.model.TunnelableData
import com.jetbrains.bigdatatools.common.connection.tunnel.model.migrateTunnel
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionConfigurable
import com.jetbrains.bigdatatools.glue.rfs.GlueDriver
import com.jetbrains.bigdatatools.glue.utils.GlueMessagesBundle
import software.amazon.awssdk.regions.Region
import javax.swing.Icon

class GlueConnectionData(@NlsSafe override var region: String = Region.US_WEST_2.id(),
                         activeAuthenticationType: String = AuthenticationType.DEFAULT.id) : TunnelableData,
                                                                                             AwsCompatibleConnectionData(
                                                                                               activeAuthenticationType,
                                                                                               connectionName = GlueMessagesBundle.message(
                                                                                                 "config.name.default")) {

  override var tunnel = ConnectionSshTunnelDataLegacy.DEFAULT

  override fun getTunnelData(): ConnectionSshTunnelData {
    migrateTunnel(this::uri)
    return super.getTunnelData()
  }

  override fun createConfigurable(project: Project, parentGroup: ConnectionGroup) =
    object : ConnectionConfigurable<GlueConnectionData, GlueSettingsCustomizer>(this, project, parentGroup.icon) {
      override fun getHelpTopic() = "big.data.tools.glue"
      override fun createSettingsCustomizer() = GlueSettingsCustomizer(project, connectionData, disposable, coroutineScope)
    }

  override fun getIcon(): Icon = BigdatatoolsGlueIcons.AwsGlue
  override fun createDriverImpl(project: Project?, isTest: Boolean): Driver = GlueDriver(this, project, isTest)
  override fun rfsDriverType() = BdtConnectionType.GLUE
}