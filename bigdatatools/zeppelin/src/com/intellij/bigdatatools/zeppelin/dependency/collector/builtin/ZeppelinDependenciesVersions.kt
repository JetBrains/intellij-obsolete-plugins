package com.intellij.bigdatatools.zeppelin.dependency.collector.builtin

import com.intellij.bigdatatools.zeppelin.settings.ZeppelinDependenciesConfig
import com.intellij.openapi.diagnostic.Logger

data class ZeppelinDependenciesVersions(val scala: String,
                                        val spark: String,
                                        val zeppelin: String,
                                        val hadoop: String,
                                        val flink: String,
                                        val avro: String = "1.7.7",
                                        val jets3: String = "0.7.1",
                                        val yarn: String = hadoop,
                                        val akka: String = "2.3.4-spark") {
  fun getZeppelinVersionAsInt() = try {
    val parts = zeppelin.split(".")
    parts[0].toInt() * 100 +
    parts[1].toInt() * 10 +
    parts[2].toInt()
  }
  catch (t: Throwable) {
    -1
  }

  companion object {
    private val logger = Logger.getInstance(this::class.java)

    fun getVersion(config: ZeppelinDependenciesConfig): ZeppelinDependenciesVersions {
      val scala = try {
        val split = config.scalaVersion.split(".")
        split[0] + "." + split[1]
      }
      catch (t: Throwable) {
        logger.warn("Cannot parse scala version for dependency resolve", t)
        config.scalaVersion
      }

      return ZeppelinDependenciesVersions(scala = scala,
                                          spark = config.sparkVersion,
                                          zeppelin = config.zeppelinVersion,
                                          hadoop = config.hadoopVersion,
                                          flink = config.flinkVersion)
    }
  }
}