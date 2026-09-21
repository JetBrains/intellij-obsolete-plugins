package com.jetbrains.bigdatatools.common.rfs.localcache.metainfo

import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.rfs.client.FileMetaInfoProvider
import com.jetbrains.bigdatatools.common.rfs.client.SchemaInfoPart
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.localcache.RfsFileContentManager

class ParquetMetaInfoProvider(private val project: Project) : FileMetaInfoProvider() {
  override fun hasMetaInfo(rfsPath: RfsPath): Boolean = rfsPath.name.endsWith(".parquet")

  override suspend fun getMetaInfo(fileInfo: FileInfo): List<SchemaInfoPart> {
    val cachedContent = RfsFileContentManager.getInstance(project).getContent(fileInfo, withMeta = true).result
    return cachedContent?.meta?.scheme ?: emptyList()
  }
}