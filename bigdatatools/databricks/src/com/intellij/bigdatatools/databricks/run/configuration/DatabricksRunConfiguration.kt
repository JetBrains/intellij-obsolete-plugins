package com.intellij.bigdatatools.databricks.run.configuration

import com.intellij.execution.Executor
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.Element

internal class DatabricksRunConfiguration(project: Project, factory: DatabricksRunConfigurationFactory, name: String)
  : LocatableConfigurationBase<RunProfileState>(project, factory, name) {

  var configurationId: String = ""

  var clusterId: String = ""

  var filePath: String = ""

  var mode = DatabricksRunMode.SERVER

  override fun writeExternal(element: Element) {
    super.writeExternal(element)
    XmlSerializer.serializeInto(this, element)
  }

  override fun readExternal(element: Element) {
    super.readExternal(element)

    try {
      XmlSerializer.deserializeInto(this, element)
    }
    catch (e: RuntimeException) {
      thisLogger().warn(e)
    }
  }

  override fun getConfigurationEditor() = DatabricksRunConfigurationEditor(project)

  override fun getState(executor: Executor, environment: ExecutionEnvironment) = DatabricksCommandLineState(project, this)

  override fun getState() = null
}