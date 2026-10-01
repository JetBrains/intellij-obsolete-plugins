package com.intellij.python.huggingFace.cacheManager.service

import com.intellij.python.community.impl.huggingFace.HuggingFaceEntityKind
import org.jetbrains.annotations.ApiStatus
import java.util.Date

@ApiStatus.Internal
data class HfCacheEntryData(
  val repoId: String,
  val repoType: HuggingFaceEntityKind,
  val sizeOnDisk: Double,
  val numFiles: Int,
  val lastAccessed: Date,
  val lastModified: Date,
  val refs: String,
  val path: String
) {
  override fun equals(other: Any?): Boolean = other is HfCacheEntryData && this.path == other.path
  override fun hashCode(): Int = path.hashCode()
}