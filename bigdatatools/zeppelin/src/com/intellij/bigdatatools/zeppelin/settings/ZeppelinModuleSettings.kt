package com.intellij.bigdatatools.zeppelin.settings

import com.intellij.bigdatatools.zeppelin.models.connection.ZeppelinInfo


data class ZeppelinDependenciesConfig(val zeppelinVersion: String,
                                      val scalaVersion: String,
                                      val sparkVersion: String,
                                      val hadoopVersion: String,
                                      val flinkVersion: String) {
  companion object {
    fun getConfig(zeppelinConnectionData: ZeppelinConnectionData, zeppelinInfo: ZeppelinInfo) = ZeppelinDependenciesConfig(
      zeppelinVersion = zeppelinInfo.version,
      scalaVersion = zeppelinConnectionData.scalaVersion,
      sparkVersion = zeppelinConnectionData.sparkVersion,
      hadoopVersion = zeppelinConnectionData.hadoopVersion,
      flinkVersion = zeppelinConnectionData.flinkVersion
    )
  }
}