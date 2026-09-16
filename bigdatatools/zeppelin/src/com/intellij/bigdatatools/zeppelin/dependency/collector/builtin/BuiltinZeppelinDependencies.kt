package com.intellij.bigdatatools.zeppelin.dependency.collector.builtin

import com.intellij.bigdatatools.zeppelin.models.SparkVersion
import com.intellij.bigdatatools.zeppelin.models.connection.ZeppelinInfo
import com.intellij.bigdatatools.zeppelin.models.interpreter.ZepDependency
import com.intellij.bigdatatools.zeppelin.settings.ZeppelinConnectionData
import com.intellij.bigdatatools.zeppelin.settings.ZeppelinDependenciesConfig

/**
 * Object resolves default Zeppelin dependencies
 */
object BuiltinZeppelinDependencies {
  /**
   * Get Zeppelin dependencies
   *
   * @param config - connection config
   * @param zeppelinInfo - runtime Zeppelin info
   *
   * @return a list of instance dependency
   */
  fun getDepsForInstance(config: ZeppelinConnectionData, zeppelinInfo: ZeppelinInfo): List<ZepDependency> {
    val depsConfig = ZeppelinDependenciesConfig.getConfig(config, zeppelinInfo)
    return getDependenciesForConfig(
      depsConfig)
  }

  /**
   * Get Zeppelin dependencies
   *
   * @param depConfig - dependency config
   * @return a list of dependency infos
   */
  private fun getDependenciesForConfig(depConfig: ZeppelinDependenciesConfig): List<ZepDependency> {
    val versions = ZeppelinDependenciesVersions.getVersion(depConfig)
    return getDefaultZeppelinDependencies(versions)
  }

  /**
   * Get Zeppelin dependencies
   *
   * @param version - get required versions of the libraries
   * @return a set with dependencies
   */
  private fun getDefaultZeppelinDependencies(version: ZeppelinDependenciesVersions): List<ZepDependency> {
    val yarnDependencies = if (SparkVersion(version.spark).olderThan(SparkVersion.SPARK_1_5_0))
      yarnDependencies(version)
    else
      emptyList()
    return zeppelinDependencies(version) +
           sparkDependencies(version) +
           hadoopDependencies(version) +
           flinkDependencies(version) +
           yarnDependencies
  }


  private fun zeppelinDependencies(version: ZeppelinDependenciesVersions) = when {
    version.zeppelin.startsWith("0.9.0") || version.zeppelin.startsWith("0.10") ->
      listOf(ZepDependency("org.apache.zeppelin:spark-scala-${version.scala}:${version.zeppelin}"))
    version.getZeppelinVersionAsInt() >= 80 ->
      listOf(ZepDependency("org.apache.zeppelin:spark-interpreter:${version.zeppelin}"))
    version.getZeppelinVersionAsInt() == -1 ->
      listOf(ZepDependency("org.apache.zeppelin:spark-interpreter:[0.8.2,)"))
    else ->
      listOf(ZepDependency("org.apache.zeppelin:zeppelin-spark_${version.scala}:${version.zeppelin}", transitive = false))
  }


  private fun hadoopDependencies(version: ZeppelinDependenciesVersions) = listOf(
    ZepDependency("org.apache.hadoop:hadoop-client:${version.hadoop}",
                  exclusions = listOf(
                    "xmlenc:xmlenc",
                    "commons-beanutils:commons-beanutils-core",
                    "xerces:xercesImpl",
                    "org.apache.httpcomponents:httpcore"
                  )
    )
  )

  private fun yarnDependencies(version: ZeppelinDependenciesVersions) = listOf(
    ZepDependency("org.apache.spark:spark-yarn_${version.scala}:${version.spark}"),
    ZepDependency("org.apache.spark:hadoop-yarn-api:${version.yarn}")
  )


  private fun sparkDependencies(version: ZeppelinDependenciesVersions): List<ZepDependency> = listOf(
    ZepDependency("org.apache.spark:spark-core_${version.scala}:${version.spark}",
                  listOf("org.apache.hadoop:hadoop-client",
                         "mx4j:mx4j",
                         "com.google.code.findbugs:jsr305"
                  )),
    ZepDependency("org.apache.spark:spark-repl_${version.scala}:${version.spark}",
                  listOf("org.apache.spark:spark-core_${version.scala}",
                         "net.sourceforge.f2j:arpack_combined_all")),
    ZepDependency("org.apache.spark:spark-sql_${version.scala}:${version.spark}",
                  listOf(
                    "org.apache.spark:spark-core_${version.scala}",
                    "org.apache.spark:spark-catalyst_${version.scala}"
                  )),
    ZepDependency("org.apache.spark:spark-hive_${version.scala}:${version.spark}",
                  listOf(
                    "org.apache.spark:spark-core_${version.scala}",
                    "org.apache.spark:spark-sql_${version.scala}",
                    "log4j:apache-log4j-extras",
                    "antlr:antlr",
                    "stax:stax-api",
                    "org.apache.derby:derby",
                    "com.google.code.findbugs:jsr305",
                    "org.apache.thrift:libfb303"
                  )),
    ZepDependency("org.apache.spark:spark-streaming_${version.scala}:${version.spark}",
                  listOf(
                    "org.apache.spark:spark-core_${version.scala}"
                  )),
    ZepDependency("org.apache.spark:spark-catalyst_${version.scala}:${version.spark}",
                  listOf(
                    "org.apache.spark:spark-core_${version.scala}"
                  ))
  )

  private fun flinkDependencies(version: ZeppelinDependenciesVersions): List<ZepDependency> {
    val scalaSuffix = if (version.flink.startsWith("1.15")) "" else "_${version.scala}"
    return listOf(
      ZepDependency("org.apache.flink:flink-table-api-java-bridge$scalaSuffix:${version.flink}"),
      ZepDependency("org.apache.flink:flink-table-api-scala-bridge_${version.scala}:${version.flink}")
    )
  }
}