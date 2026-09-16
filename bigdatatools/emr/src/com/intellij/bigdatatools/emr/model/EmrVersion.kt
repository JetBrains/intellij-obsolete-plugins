package com.intellij.bigdatatools.emr.model

class EmrVersion(private val rawVersion: String) : Comparable<EmrVersion> {
  val x = rawVersion.removePrefix("emr-").split(".").first()
  val y = rawVersion.removePrefix("emr-").split(".")[1]
  val z = rawVersion.removePrefix("emr-").split(".").last()

  override fun compareTo(other: EmrVersion) = comparator.compare(this, other)

  companion object {
    val comparator = compareBy(EmrVersion::x, EmrVersion::y, EmrVersion::z)
  }
}