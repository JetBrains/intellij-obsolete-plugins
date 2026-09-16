package com.intellij.bigdatatools.plugin.spark.arbitrary.wizard

import com.intellij.bigdatatools.plugin.spark.arbitrary.ArbitraryClusterConnectionData
import com.intellij.bigdatatools.plugin.spark.arbitrary.utils.ArbitraryClusterUtils
import com.intellij.execution.target.TargetEnvironmentConfiguration
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.project.Project
import com.intellij.ssh.config.unified.SshConfig
import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.jetbrains.bigdatatools.common.rfs.driver.SafeExecutor
import com.jetbrains.bigdatatools.sftp.settings.SftpSettingsCustomizer
import com.jetbrains.spark.monitoring.settings.SparkSettingsCustomizer
import kotlinx.coroutines.plus

class ArbitraryClusterTargetEnvConfiguration(val project: Project,
                                             val connectionData: ArbitraryClusterConnectionData,
                                             val disposable: Disposable) :
  TargetEnvironmentConfiguration(Utils.ID) {

  val coroutineScope = SafeExecutor.createInstance(disposable).coroutineScope.plus(ModalityState.any().asContextElement())
  val sparkConnectionData = ArbitraryClusterUtils.createSparkConnection(connectionData)
  val sftpConnectionData = ArbitraryClusterUtils.createSftp(connectionData)

  val sparkSettingsCustomizer = SparkSettingsCustomizer(project, sparkConnectionData,
                                                        disposable,
                                                        coroutineScope)

  val sftpSettingsCustomizer = SftpSettingsCustomizer(project, sftpConnectionData,
                                                      disposable)


  var selectedSparkType: DependConnectionType = DependConnectionType.DEFAULT
  var selectedSftpType: DependConnectionType = DependConnectionType.DEFAULT

  val selectedSshConfig = AtomicProperty<SshConfig?>(null)
  val selectedSshConfigLabel = selectedSshConfig.transform { config ->
    config?.presentableShortName ?: MessagesBundle.message("combobox.item.is.not.selected")
  }


  object Utils {
    const val ID = "arbitrary-cluster-new"
  }

  override var projectRootOnTarget: String = ""
}