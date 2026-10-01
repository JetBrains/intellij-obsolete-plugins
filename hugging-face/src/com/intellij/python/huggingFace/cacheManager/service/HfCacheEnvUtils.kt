package com.intellij.python.huggingFace.cacheManager.service

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.ide.plugins.PluginManagerCore.logger
import com.intellij.python.community.impl.huggingFace.service.HuggingFaceCoroutine
import com.intellij.util.SystemProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

object HfCacheEnvUtils {
  const val HUGGINGFACE_CLI: String = "huggingface-cli"

  /**
   * Recapitulates how src/huggingface_hub/constants.py:112 (HF_HUB_CACHE)
   * is defined in the huggingface_hub repo.
   */
  fun getCacheDir(): Result<File?> = runCatching {
    // recapitulates huggingface_hub.utils._cache_manager.scan_cache_dir
    val defaultHome = Paths.get(SystemProperties.getUserHome(), ".cache").toString()
    val hfHome = System.getenv("HF_HOME") ?: Paths.get(System.getenv("XDG_CACHE_HOME") ?: defaultHome, "huggingface").toString()
    val defaultCachePath = Paths.get(hfHome, "hub").toString()
    val hfHubCache = System.getenv("HUGGINGFACE_HUB_CACHE") ?: defaultCachePath
    val cacheDirPath = System.getenv("HF_HUB_CACHE") ?: hfHubCache

    val cacheDir = File(cacheDirPath)
    if (!cacheDir.exists() || !cacheDir.isDirectory) return@runCatching null
    cacheDir
  }.onFailure {
    it.printStackTrace()
  }

  @Suppress("unused")
  private fun isHuggingFaceCliInstalled(): Result<Boolean> = runCatching {
    val cmd = GeneralCommandLine()
    cmd.withExePath(HUGGINGFACE_CLI)
    cmd.addParameters("-h")
    val out = ExecUtil.execAndGetOutput(cmd)
    val exitCode = out.exitCode
    exitCode == 0
  }.onFailure {
    it.printStackTrace()
  }

  private suspend fun removeFilesForItem(item: HfCacheEntryData) = withContext(Dispatchers.IO) {
      val dirPath = Paths.get(item.path)
      if (!Files.exists(dirPath)) return@withContext
      Files.walk(dirPath)
        .sorted(Comparator.reverseOrder())
        .forEach(Files::delete)
    }

  fun doDeleteItemAndUpdateTable(selectedItem: HfCacheEntryData, tableDataUpdater: () -> Unit): Job = HuggingFaceCoroutine.Utils.ioScope.launch {
    try {
      removeFilesForItem(selectedItem)
      withContext(Dispatchers.Main) {
        HfCacheScanner.instance.clearCacheFor(selectedItem)
        tableDataUpdater()
      }
    } catch (e: IOException) {
      logger.warn("Unable to delete directory: ${selectedItem.path}", e)
    }
  }
}
