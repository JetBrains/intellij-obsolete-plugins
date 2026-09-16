package com.intellij.bigdatatools.zeppelin.models.notebook

import com.intellij.bigdatatools.zeppelin.rfs.path.ZeppelinRfsPath
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.search.impl.SearchElement
import javax.swing.Icon

data class ZeppelinFullTextSearchElement(override val id: String,
                                         override val fileName: String = "",
                                         override val query: String = "",
                                         override val name: String = "",
                                         override val snippet: String = "",
                                         override val text: String = "",
                                         override var connId: String = "",
                                         override val header: String = "",
                                         override val fileInfo: FileInfo? = null) : SearchElement {
  override val rfsPath: RfsPath
    get() = ZeppelinRfsPath.createRfsPath(id, fileName)
  override var icon: Icon? = null
}