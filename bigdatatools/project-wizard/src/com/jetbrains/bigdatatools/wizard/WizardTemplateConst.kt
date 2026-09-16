package com.jetbrains.bigdatatools.wizard

object WizardTemplateConst {
  fun getSparkVersionFor(scalaVersionsShort: String): String {
    return when (scalaVersionsShort) {
      "2.13" -> "3.5.0"
      "2.12" -> "3.5.0"
      "2.11" -> "2.4.8"
      "2.10" -> "2.2.3"
      else -> "3.5.0"
    }
  }

  const val ARTIFACT = "ARTIFACT"

  const val SPARK_VERSION = "SPARK_VERSION"

  const val SCALA_VERSIONS_SHORT = "SCALA_VERSION"
  const val SCALA_VERSION_SHORT_DEFAULT = "2.12"

  const val SCALA_FULL_VERSION = "SCALA_FULL_VERSION"
  const val SCALA_FULL_VERSION_DEFAULT = "2.12.10"

  const val SBT_VERSION = "SBT_VERSION"
  const val SBT_VERSION_DEFAULT = "1.9.6"

  const val GROUP_ID = "GROUP_ID"

  const val PACKAGE = "FILE_PACKAGE"
}