package com.intellij.python.huggingFace.cacheManager.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.python.community.impl.huggingFace.HuggingFaceEntityKind
import com.intellij.python.community.impl.huggingFace.api.HuggingFaceApi
import com.intellij.python.community.impl.huggingFace.cache.HuggingFaceModelsCache
import com.intellij.python.community.impl.huggingFace.service.HuggingFaceCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.attribute.BasicFileAttributes
import java.util.Date

@Service(Service.Level.APP)
class HfCacheScanner {
  private val cachedData = mutableSetOf<HfCacheEntryData>()

  fun updateCachedData(onUpdate: () -> Unit ) {
    // see huggingface_hub.utils._cache_manager._scan_cached_repo
    HuggingFaceCoroutine.Utils.ioScope.launch {
      cachedData.clear()

      val cacheDirResult = HfCacheEnvUtils.getCacheDir()
      val cacheDir = cacheDirResult.getOrNull()

      if (!cacheDirResult.isSuccess || cacheDir == null) {
        withContext(Dispatchers.Main) { onUpdate() }
        return@launch
      }

      val allCacheDirs = cacheDir.listFiles { file -> file.isDirectory && file.name.split("--").size >= 3 }

      allCacheDirs?.forEach { directory ->
        val directoryAttributes = Files.readAttributes(directory.toPath(), BasicFileAttributes::class.java)
        val directorySize = Files.walk(directory.toPath())
          .filter { path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) }
          .mapToLong { path -> Files.size(path) }
          .sum()
          .toDouble()

        val repoIDParts = directory.name.split("--")
        val repoType = HuggingFaceEntityKind.entries.firstOrNull { it.urlFragment == repoIDParts[0] } ?: return@forEach
        val repoId = "${repoIDParts[1]}/${repoIDParts[2]}"

        val numberOfFiles = runCatching {
          Files.walk(directory.toPath())
            .filter { path -> Files.isRegularFile(path) && !Files.isSymbolicLink(path) && !Files.isDirectory(path) && Files.size(path) > 0 }
            .toList()
            .size
        }.getOrElse { 0 }

        val data = HfCacheEntryData(
          repoId = repoId,
          repoType = repoType,
          sizeOnDisk = directorySize,
          numFiles = numberOfFiles,
          lastAccessed = Date(directoryAttributes.lastAccessTime().toMillis()),
          lastModified = Date(directoryAttributes.lastModifiedTime().toMillis()),
          refs = "main",  // todo: check whether it's indeed needed
          path = directory.path
        )

        cachedData.add(data)
      }

      withContext(Dispatchers.Main) { onUpdate() }
      updateCardsCache()
    }
  }

  private suspend fun updateCardsCache() {
    cachedData.forEach { item ->
      if (item.repoType != HuggingFaceEntityKind.MODEL) return
      if (HuggingFaceModelsCache.isInCache(item.repoId)) return
      val modelData = HuggingFaceApi.fetchDataForSingleModel(item.repoId)
      modelData?.let { HuggingFaceModelsCache.saveEntity(it) }
    }
  }

  fun getCachedData(): List<HfCacheEntryData> = cachedData.toList()
  fun clearCacheFor(item: HfCacheEntryData): Boolean = cachedData.remove(item)

  companion object {
    val instance: HfCacheScanner
      get() = service<HfCacheScanner>()
  }
}
