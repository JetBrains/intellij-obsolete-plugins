package com.jetbrains.bigdatatools.common.rfs.localcache.metainfo

import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.rfs.client.FileMetaInfoProvider
import com.jetbrains.bigdatatools.common.rfs.client.SchemaInfoPart
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.localcache.RfsFileContentManager

/**
 * User: Dmitry.Naydanov
 * Date: 26.08.18.
 */
class CsvMetaInfoProvider(private val project: Project) : FileMetaInfoProvider() {
  override fun hasMetaInfo(rfsPath: RfsPath): Boolean = rfsPath.name.endsWith(".csv")

  override suspend fun getMetaInfo(fileInfo: FileInfo): List<SchemaInfoPart> {
    val contentSafe = RfsFileContentManager.getInstance(project).getContent(fileInfo, withMeta = true)
    return contentSafe.result?.meta?.scheme ?: emptyList()
  }
}