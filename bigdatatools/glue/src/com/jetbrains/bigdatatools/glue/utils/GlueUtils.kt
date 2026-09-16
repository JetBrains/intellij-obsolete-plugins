package com.jetbrains.bigdatatools.glue.utils

import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath

object GlueUtils {
  val RfsPath.database: String?
    get() = this.elements.getOrNull(0)
  val RfsPath.table: String?
    get() = this.elements.getOrNull(1)
  val RfsPath.schema: String?
    get() = this.elements.getOrNull(2)

  val RfsPath.isSchema: Boolean
    get() = this.size == 3
  val RfsPath.isTable: Boolean
    get() = this.size == 2
  val RfsPath.isDatabase: Boolean
    get() = this.size == 1

}