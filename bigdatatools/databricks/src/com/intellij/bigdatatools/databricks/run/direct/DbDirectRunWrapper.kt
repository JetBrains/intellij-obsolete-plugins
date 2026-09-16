package com.intellij.bigdatatools.databricks.run.direct

import com.intellij.bigdatatools.databricks.client.DatabricksDataManager
import com.intellij.bigdatatools.databricks.model.withWorkspace
import com.intellij.openapi.project.Project
import com.intellij.util.ResourceUtil
import com.intellij.bigdatatools.coreUi.serializer.BdtJson
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath

internal class DbDirectRunWrapper(private val project: Project,
                         private val dataManager: DatabricksDataManager) {
  fun createPyWrapperText(rfsPath: RfsPath, args: List<String>, envVars: Map<String, Any>): String {
    val workspacePath = "\"" + rfsPath.withWorkspace + "\""

    val syncMapper = dataManager.syncManager.getTaskForProject(project).syncMapper
    val preparedArgs = listOf(workspacePath) + args

    for (key in envVars.keys) {
      if (!Regex("^[a-zA-Z_]{1,}[a-zA-Z0-9_]*$").matches(key)) {
        throw Exception("Invalid environment variable $key: Only lower and upper case letters, digits and '_'(underscore) are allowed.")
      }
    }

    var text = ResourceUtil.getResourceAsStream(this::class.java.classLoader,
                                                "wrapper",
                                                "bootstrap.py").reader().readText()

    text = text.replace("\"PYTHON_FILE\"", workspacePath)
    text = text.replace("\"REPO_PATH\"", "\"" + syncMapper.baseRemotePath.withWorkspace + "\"")
    text = text.replace("args = []", "args = [${preparedArgs.joinToString(",") { escapePythonString(it) }}]")
    text = text.replace("env = {}", "env = ${BdtJson.toJson(envVars, pretty = false)}")

    return text
  }


  private fun escapePythonString(str: String) = str.replace("\\", "\\\\").replace("'", "\\'")
}