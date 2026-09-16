package com.intellij.bigdatatools.emr.util

import com.intellij.bigdatatools.emr.model.EmrClusterAppInfo
import com.intellij.bigdatatools.emr.model.EmrClusterDetails
import com.intellij.bigdatatools.emr.model.EmrVersion
import com.intellij.openapi.diagnostic.thisLogger
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.constants.BdtPlugins
import software.amazon.awssdk.services.emr.model.Application
import software.amazon.awssdk.services.emr.model.Cluster

object EmrClusterAppUtils {
  fun getFor(appInfo: Application, clusterDetails: EmrClusterDetails): List<EmrClusterAppInfo> {
    val cluster = clusterDetails.cluster

    val url = clusterDetails.cluster.masterPublicDnsName()

    val version = EmrVersion(cluster.releaseLabel())
    return when (appInfo.name()) {
      "Flink" -> getFlinkApp(version, appInfo, cluster)
      "Ganglia" -> listOf(
        EmrClusterAppInfo(name = "Ganglia", version = appInfo.version(), url = "$url/ganglia", connType = null)
      )
      "Hadoop" -> getHadoopApps(appInfo, cluster)
      "HBase" -> listOf(
        EmrClusterAppInfo(name = "HBase", version = appInfo.version(), url = "$url:16010", connType = null),
      )
      "Hue" -> listOf(
        EmrClusterAppInfo(name = "Hue", version = appInfo.version(), url = "$url:8888", null),
      )
      "JupyterHub" -> listOf(
        EmrClusterAppInfo(name = "JupyterHub", version = appInfo.version(), url = "$url:9443", null)
      )
      "Livy" -> listOf(
        EmrClusterAppInfo(name = "Livy", version = appInfo.version(), url = "$url:8998", null),
      )
      "Spark" -> listOf(
        getSparkInfo(appInfo, cluster)
      )
      "Tez" -> listOf(
        EmrClusterAppInfo(name = "Tez", version = appInfo.version(), url = "$url:8080/tez-ui", null),
      )
      "Zeppelin" -> listOf(
        EmrClusterAppInfo(name = "Zeppelin", version = appInfo.version(), url = "$url:8890", connType = BdtConnectionType.ZEPPELIN,
                          additionalParams = getZeppelinVersionInfo(cluster)),
      )
      "Hive" -> getHiveApps(appInfo, cluster)
      else -> listOf(EmrClusterAppInfo(name = appInfo.name(), version = appInfo.version(), url = "", connType = null))
    }
  }

  fun getSparkInfo(clusterDetails: EmrClusterDetails): EmrClusterAppInfo {
    return getSparkInfo(Application.builder().version("").name("Spark").build(), clusterDetails.cluster)
  }

  private fun getSparkInfo(appInfo: Application,
                           cluster: Cluster): EmrClusterAppInfo {
    val url = cluster.masterPublicDnsName()
    val sparkAppInfo = EmrClusterAppInfo(name = "Spark History Server", version = appInfo.version(), url = "$url:18080",
                                         connType = BdtConnectionType.SPARK_MONITORING)
    return if (BdtPlugins.isSparkPluginInstalled())
      sparkAppInfo
    else
      sparkAppInfo.copy(connType = null)
  }

  private fun getHiveApps(appInfo: Application,
                          cluster: Cluster): List<EmrClusterAppInfo> {
    val url = cluster.masterPublicDnsName()
    return listOf(
      EmrClusterAppInfo(name = "Hive Metastore", version = appInfo.version(), url = "$url:9083", connType = BdtConnectionType.HIVE)
    )
  }

  private fun getHadoopApps(appInfo: Application, cluster: Cluster): List<EmrClusterAppInfo> {
    val version = EmrVersion(cluster.releaseLabel())
    val nameNodePort = if (version < EmrVersion("emr-6.0.0")) "50470" else "9870"
    val datanodePort = if (version < EmrVersion("emr-6.0.0")) "50075" else "9864"
    val url = cluster.masterPublicDnsName()
    return listOf(
      EmrClusterAppInfo(name = "HDFS Connection", version = appInfo.version(), url = "$url:8020", connType = BdtConnectionType.HDFS,
                        additionalParams = mapOf("user" to "hadoop")),
      EmrClusterAppInfo(name = "HDFS NameNode Web", version = appInfo.version(), url = "$url:$nameNodePort", connType = null),
      EmrClusterAppInfo(name = "HDFS DataNode Web", version = appInfo.version(), url = "$url:$datanodePort", connType = null),
      EmrClusterAppInfo(name = "YARN ResourceManager", version = appInfo.version(), url = "$url:8088", connType = BdtConnectionType.YARN),
      EmrClusterAppInfo(name = "YARN NodeManager", version = appInfo.version(), url = "$url:8042", connType = null)
    )
  }

  private fun getFlinkApp(version: EmrVersion,
                          appInfo: Application,
                          cluster: Cluster) = if (version >= EmrVersion("emr-5.33.0")) {
    val url = cluster.masterPublicDnsName()
    val flinkInfo = EmrClusterAppInfo(name = "Flink history server", version = appInfo.version(), url = "$url:8082",
                                      connType = BdtConnectionType.FLINK)
    listOf(flinkInfo)
  }
  else
    emptyList()


  private fun getZeppelinVersionInfo(clusterInfo: Cluster): Map<String, String> {
    val sparkVersion = clusterInfo.applications().find { it.name() == "Spark" }?.version() ?: "2.7.3"
    val hadoopVersion = clusterInfo.applications().find { it.name() == "Hadoop" }?.version() ?: "2.7.3"

    val emrVersion = clusterInfo.releaseLabel().removePrefix("emr-")
    val scalaVersion = when {
      emrVersion.startsWith("6") -> "2.12"
      emrVersion.startsWith("5") -> "2.11"
      emrVersion.startsWith("4") -> "2.10"
      else -> {
        thisLogger().error("Cannot detect Scala version for EMR Version: ${clusterInfo.releaseLabel()}. Please write us!")
        "2.11"
      }
    }

    return mapOf(
      "sparkVersion" to sparkVersion,
      "hadoopVersion" to hadoopVersion,
      "scalaVersion" to scalaVersion,
    )
  }

}