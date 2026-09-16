package com.intellij.bigdatatools.zeppelin.settings

internal data class LibraryVersion(val raw: String) : Comparable<LibraryVersion> {
  val x = raw.takeWhile { it != '.' }.toInt()
  val y = raw.removePrefix("${x}.").takeWhile { it != '.' }.toInt()
  val z = raw.removePrefix("${x}.").takeLastWhile { it != '.' }.toInt()

  override fun compareTo(other: LibraryVersion) = comparator.compare(this, other)

  companion object {
    fun getSupportedScalaVersion() = arrayOf("2.10", "2.11", "2.12")
    val comparator = compareBy(LibraryVersion::x, LibraryVersion::y, LibraryVersion::z)
  }
}