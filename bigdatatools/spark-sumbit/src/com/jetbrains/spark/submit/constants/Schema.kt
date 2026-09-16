package com.jetbrains.spark.submit.constants

internal object Schema {
  const val SSH_CONFIG_ID = "SSH_CONFIG_ID"
  const val CLUSTER_MANAGER = "ClusterManager"
  const val ARTIFACT_PATH = "JarPath"
  const val CLASS_NAME = "ClassName"
  const val ARTIFACT_ARGS = "ARTIFACT_ARGS"
  const val JARS = "Jars"
  const val FILES = "FILES"
  const val PY_FILES = "PyFiles"
  const val PROPERTIES_FILE = "PropertiesFile"
  const val CONF = "CONF"
  const val VERBOSE = "Verbose"
  const val DEPLOY_MODE = "DeployMode"

  const val PACKAGES = "Packages"
  const val EXCLUDE_PACKAGES = "ExcludePackages"
  const val REPOSITORIES = "Repositories"

  const val DRIVER_MEMORY = "DRIVER_MEMORY"
  const val DRIVER_JAVA_OPTIONS = "DRIVER_JAVA_OPTIONS"
  const val DRIVER_LIBRARY_PATH = "DRIVER_LIBRARY_PATH"
  const val DRIVER_CLASS_PATH = "DRIVER_CLASS_PATH"
  const val DRIVER_CORES = "DRIVER_CORES"


  //Executor settings
  const val EXECUTOR_MEMORY = "EXECUTOR_MEMORY"
  const val TOTAL_EXECUTORS_CORES = "TOTAL_EXECUTORS_CORES"
  const val NUM_EXECUTORS = "NUM_EXECUTORS"
  const val EXECUTOR_CORES = "EXECUTOR_CORES"
  const val ARCHIVES = "ARCHIVES"

  const val PRINCIPAL = "principal"
  const val KEYTAB = "keytab"
  const val SPARK_MONITORING = "SPARK_MONITORING"

  const val SPARK_HOME_PATH = "SparkHomePath"
  const val MASTER = "Master"
  const val SUPERVISE = "Supervise"
  const val QUEUE = "QUEUE"
  const val PROXY_USER = "PROXY_USER"
}