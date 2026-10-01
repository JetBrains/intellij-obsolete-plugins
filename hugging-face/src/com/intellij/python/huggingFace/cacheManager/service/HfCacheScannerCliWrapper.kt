package com.intellij.python.huggingFace.cacheManager.service

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.python.community.impl.huggingFace.HuggingFaceEntityKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import kotlin.math.pow

@Suppress("unused")
class HfCacheScannerCliWrapper {
  suspend fun getCacheContent(): Result<List<HfCacheEntryData>> = withContext(Dispatchers.IO) {
    runCatching {
      val cmdOut = ExecUtil.execAndGetOutput(scanCacheCommand())
      val outputLines = cmdOut.stdoutLines.drop(2).dropLast(2)
      outputLines.mapNotNull { line ->
        val splitList = line.split(Regex("\\s+"))
        parseCacheElement(splitList)
      }
    }.onFailure {
      thisLogger().error("Failed to retrieve huggingface-cli cache-data", it)
    }
  }

  private fun parseCacheElement(splitList: List<String>): HfCacheEntryData? {
    val repoId = splitList[0]
    val repoType = when (splitList[1]) {
      "model" -> HuggingFaceEntityKind.MODEL
      "dataset" -> HuggingFaceEntityKind.DATASET
      "space" -> HuggingFaceEntityKind.SPACE
      else -> return null
    }
    val sizeOnDiskMb = parseSize(splitList[2])
    val numFiles = splitList[3].toInt()

    val lastAccessed = getApproximatePastDate(splitList[4].toLong(), splitList[5])
    val lastModified = getApproximatePastDate(splitList[7].toLong(), splitList[8])

    val refs = splitList[10]
    val path = splitList[11]
    return HfCacheEntryData(repoId, repoType, sizeOnDiskMb, numFiles, lastAccessed, lastModified, refs, path)
  }

  /**
   * Reverses huggingface_hub.utils._cache_manager._format_size
   */
  private fun parseSize(sizeStr: String): Double {
    val units = listOf("", "K", "M", "G", "T", "P", "E", "Z")
    val unit = if (units.contains(sizeStr.last().toString())) sizeStr.last().toString() else ""
    val size = if (unit.isNotEmpty()) sizeStr.dropLast(1).toDouble() else sizeStr.toDouble()

    val unitIndex = units.indexOf(unit)
    if (unitIndex < 0) return 0.0  // todo: proper handling

    // Adjust the power factor to reflect the conversion to megabytes
    val sizeMb = size * 1024.0.pow(unitIndex - 2.0)
    return sizeMb
  }

  /**
   * Reverses huggingface_hub.utils._cache_manager._format_timesince
   * that control how the LAST_ACCESSED and LAST_MODIFIED fields are formatted.
   */
  private fun getApproximatePastDate(number: Long, unit: String): Date {
    val now = LocalDateTime.now()
    val localDateTime = when (unit) {
      "second" -> now.minusSeconds(number)
      "seconds" -> now.minusSeconds(number)
      "minute" -> now.minusMinutes(number)
      "minutes" -> now.minusMinutes(number)
      "hour" -> now.minusHours(number)
      "hours" -> now.minusHours(number)
      "day" -> now.minusDays(number)
      "days" -> now.minusDays(number)
      "week" -> now.minusWeeks(number)
      "weeks" -> now.minusWeeks(number)
      "month" -> now.minusMonths(number)
      "months" -> now.minusMonths(number)
      "year" -> now.minusYears(number)
      "years" -> now.minusYears(number)
      else -> throw IllegalArgumentException("Unknown time unit: $unit")
    }
    return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant())
  }

  private fun scanCacheCommand(): GeneralCommandLine {
    val cmd = GeneralCommandLine()
    cmd.withExePath(HfCacheEnvUtils.HUGGINGFACE_CLI)
    cmd.addParameters("scan-cache")
    return cmd
  }
}