package com.jetbrains.bigdatatools.dataproc.model

class DataprocVersion(private val rawVersion: String) : Comparable<DataprocVersion> {
  val x = rawVersion.removePrefix("emr-").split(".").first()
  val y = rawVersion.removePrefix("emr-").split(".")[1]
  val z = rawVersion.removePrefix("emr-").split(".").last()

  override fun compareTo(other: DataprocVersion) = comparator.compare(this, other)

  companion object {
    val comparator = compareBy(DataprocVersion::x, DataprocVersion::y, DataprocVersion::z)
  }
}