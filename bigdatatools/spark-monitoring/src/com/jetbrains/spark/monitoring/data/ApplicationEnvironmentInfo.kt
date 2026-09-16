package com.jetbrains.spark.monitoring.data

import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo

// http://localhost:4040/api/v1/applications/local-1558513925500/environment
data class ApplicationEnvironmentInfo(
  val runtime: RuntimeInfo,
  val sparkProperties: List<List<String>>, // this is array [[key, value], [key, value]]
  val hadoopProperties: List<List<String>> = emptyList(), // this is array [[key, value], [key, value]]
  val systemProperties: List<List<String>> = emptyList(), // this is array [[key, value], [key, value]]
  val classpathEntries: List<List<String>> = emptyList() // this is array [[key, value], [key, value]]
  //It is not used in our UI
  //val resourceProfiles: List<ResourceProfileInfo> = emptyList()
) : RemoteInfo
