package com.jetbrains.bigdatatools.hivemetastore.utils

import com.intellij.bigdatatools.coreUi.util.BdtUrlUtils
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.util.toPresentableText
import java.net.URI

object HiveMetastoreUtils {
  val RfsPath.catalog: String?
    get() = this.elements.getOrNull(0)
  val RfsPath.database: String?
    get() = this.elements.getOrNull(1)
  val RfsPath.table: String?
    get() = this.elements.getOrNull(2)
  val RfsPath.schema: String?
    get() = this.elements.getOrNull(3)

  val RfsPath.isSchema: Boolean
    get() = this.size == 4
  val RfsPath.isTable: Boolean
    get() = this.size == 3
  val RfsPath.isDatabase: Boolean
    get() = this.size == 2
  val RfsPath.isCatalog: Boolean
    get() = this.size == 1


  fun validateHiveUrls(url: String): String? {
    return try {
      if (url.isBlank())
        return HiveMessagesBundle.message("error.uris.not.found")

      val urls = url.split(",").map { it.trim() }.filter { it.isNotBlank() }
      urls.firstNotNullOfOrNull {

        val error = try {
          URI(it)
          null
        }
        catch (t: Throwable) {
          t.toPresentableText()
        }

        if (error != null)
          "$it: $error"
        else {
          try {
            val protocol = BdtUrlUtils.getProtocol(it)
            if (protocol.isNotBlank() && protocol != "thrift")
              "$it: " + HiveMessagesBundle.message("error.validation.protocol")
            else
              null
          }
          catch (t: Throwable) {
            "$it: ${t.toPresentableText()}"
          }
        }
      }
    }
    catch (t: Throwable) {
      t.toPresentableText()
    }
  }
}