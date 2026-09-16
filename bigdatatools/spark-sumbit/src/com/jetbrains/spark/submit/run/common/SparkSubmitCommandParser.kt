package com.jetbrains.spark.submit.run.common

import com.intellij.execution.configurations.ParametersList
import com.jetbrains.spark.submit.model.ClusterManagerType
import com.jetbrains.spark.submit.model.DeployModeType
import com.jetbrains.spark.submit.model.FilePath

class SparkSubmitCommandParser(paramString: String, private val configuration: AbstractSparkJobRunConfiguration<*>) {
  private val preparedString = paramString.replace("\\\n", " ").replace("[ \t\n]+", "")
  private val params: Array<String> = ParametersList.parse(preparedString)

  private var maxIndex = params.lastIndex
  private var curIndex = 0


  private var sparkConfParams = mutableListOf<String>()

  fun parse() {
    if (maxIndex == 0)
      return

    setSparkHome()
    setSparkSubmitParams()


    setArtifact()
    setArtifactArgs()
  }

  private fun setArtifact() {
    if (curIndex > maxIndex)
      return

    val paramValue = params[curIndex]
    curIndex++
    configuration.artifactPath = FilePath.fromPathWithScheme(paramValue)
  }

  private fun setArtifactArgs() {
    val args = params.toList().subList(curIndex, maxIndex + 1)
    configuration.artifactArgs = ParametersList.join(args)
  }


  private fun setSparkSubmitParams() {
    @Suppress("ControlFlowWithEmptyBody")
    while (processParam()) {
    }
    configuration.conf = ParametersList.join(sparkConfParams)
  }

  private fun setSparkHome() {
    configuration.sparkHome = params.first().removeSuffix("bin/spark-submit").removeSuffix("/")
    curIndex++
  }

  private fun processParam(): Boolean {
    if (curIndex > maxIndex) {
      return false
    }

    return when (params[curIndex]) {
      SparkSchema.CLAZZ -> setParamString {
        configuration.className = it
      }
      SparkSchema.DEPLOY_MODE -> setParamString { param ->
        configuration.clusterManagerDeployModeDefault = false
        val deployModeToSet = DeployModeType.entries.firstOrNull { it.valueForCommand.equals(param, ignoreCase = true) }
        if (deployModeToSet != null) {
          configuration.deployMode = deployModeToSet
        }
      }
      SparkSchema.MASTER -> setParamString { param ->
        configuration.clusterManagerDeployModeDefault = false
        configuration.master = param
        configuration.clusterManager = ClusterManagerType.fromMaster(param)
      }
      SparkSchema.NAME -> setParamString {
        configuration.name = it
      }

      SparkSchema.VERBOSE, "-v" -> {
        configuration.verbose = true
        curIndex++
        true
      }
      SparkSchema.SUPERVISE -> {
        configuration.supervise = true
        curIndex++
        true
      }

      SparkSchema.KEYTAB -> setParamPath {
        configuration.keytab = it
      }
      SparkSchema.PRINCIPAL -> setParamString {
        configuration.principal = it
      }

      SparkSchema.DRIVER_CORES -> setParamString {
        configuration.driverCores = it
      }
      SparkSchema.DRIVER_MEMORY -> setParamString {
        configuration.driverMemory = it
      }
      SparkSchema.DRIVER_JAVA_OPTS -> setParamString {
        configuration.driverJavaOptions = it
      }
      SparkSchema.DRIVER_LIB_PATH -> setParamListPath {
        configuration.driverLibraryPath = it
      }
      SparkSchema.DRIVER_CLASS_PATH -> setParamListPath {
        configuration.driverClassPath = it
      }

      SparkSchema.EXECUTOR_MEMORY -> setParamString {
        configuration.executorMemory = it
      }
      SparkSchema.NUM_EXECUTORS -> setParamString {
        configuration.numExecutors = it
      }
      SparkSchema.EXECUTOR_CORES -> setParamString {
        configuration.executorCores = it
      }
      SparkSchema.TOTAL_EXECUTOR_CORES -> setParamString {
        configuration.totalExecutorCores = it
      }

      SparkSchema.REPOSITORIES -> setParamString {
        configuration.repositories = it
      }
      SparkSchema.PACKAGES -> setParamString {
        configuration.packages = it
      }
      SparkSchema.EXCLUDE_PACKAGES -> setParamString {
        configuration.excludePackages = it
      }

      SparkSchema.JARS -> setParamListPath {
        configuration.jars = it
      }
      SparkSchema.PY_FILES -> setParamListPath {
        configuration.pyFiles = it
      }
      SparkSchema.FILES -> setParamListPath {
        configuration.files = it
      }
      SparkSchema.ARCHIVES -> setParamListPath {
        configuration.archives = it
      }

      SparkSchema.PROPERTIES_FILE -> setParamPath {
        configuration.propertiesFile = it
      }
      SparkSchema.CONF_PARAM -> setParamString {
        sparkConfParams.add(it)
      }

      SparkSchema.PROXY_USER -> setParamString {
        configuration.proxyUser = it
      }
      SparkSchema.QUEUE -> setParamString {
        configuration.queue = it
      }
      else -> false
    }
  }

  private fun setParamString(setFun: (String) -> Unit) = setParamArg {
    setFun(it)
    true
  }

  private fun setParamListPath(setFun: (List<FilePath>) -> Unit) = setParamArg { param ->
    val stringPaths = param.split(",").map { it.trim() }
    val paths = stringPaths.map { FilePath.fromPathWithScheme(it) }
    setFun(paths)
    true
  }

  private fun setParamPath(setFun: (FilePath) -> Unit): Boolean = setParamArg {
    val filePath = FilePath.fromPathWithScheme(it)
    setFun(filePath)
    true
  }


  private fun setParamArg(setFun: (String) -> Boolean): Boolean {
    if (curIndex > maxIndex)
      return false
    curIndex++
    val paramValue = params[curIndex]
    curIndex++
    return setFun(paramValue)
  }
}