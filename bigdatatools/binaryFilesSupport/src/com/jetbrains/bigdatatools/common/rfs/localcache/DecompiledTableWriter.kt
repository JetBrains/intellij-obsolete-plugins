package com.jetbrains.bigdatatools.common.rfs.localcache

import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.limits.FileSizeLimit
import java.io.File
import java.io.FileWriter
import java.nio.charset.StandardCharsets

internal object DecompiledTableWriter {
  fun write(
    file: File,
    columns: Array<String>,
    iterators: List<Pair<Int, Iterator<String>>>,
    entriesLimit: Int,
  ): List<Int> {
    val iterator = TotalSizeLimitingIterator(
      FileSizeLimit.getContentLoadLimit(file.extension) - columns.sumOf { it.length },
      CompoundIterator(iterators.sortedBy { it.first }.map { it.second }, "<absent>", 2000),
    )

    return write(file, columns, iterator, entriesLimit)
  }

  fun write(
    file: File,
    columns: Array<String>,
    iterator: Iterator<List<String>>,
    entriesLimit: Int,
  ): List<Int> {
    FileWriter(file, StandardCharsets.UTF_8).use { writer ->
      var offset = 0
      val offsets = mutableListOf<Int>()

      fun addOffset(value: String, newLine: Boolean = false) {
        offset += value.length
        offsets.add(if (newLine) -offset else offset)
        offset++
      }

      if (columns.isNotEmpty()) {
        for (i in 0..columns.size - 2) {
          writer.write(columns[i])
          writer.write(",")
          addOffset(columns[i])
        }
        writer.write(columns[columns.size - 1])
        writer.write("\n")
        addOffset(columns[columns.size - 1], true)
      }

      val lines = if (entriesLimit > 0) LimitedIterator(iterator, entriesLimit) else iterator

      for (line in lines) {
        val values = line.iterator()
        while (values.hasNext()) {
          val next = StringUtil.escapeStringCharacters(values.next())
          writer.write(next)
          if (values.hasNext()) writer.write(",") else writer.write("\n")
          addOffset(next, !values.hasNext())
        }
      }

      return offsets
    }
  }
}

private class CompoundIterator<T>(
  private val iterators: Collection<Iterator<T>>,
  private val defaultValue: T,
  private val minEntries: Int = -1,
) : Iterator<List<T>> {
  private var count = 0

  override fun hasNext(): Boolean =
    iterators.any { it.hasNext() } && (minEntries == -1 || count <= minEntries) || iterators.all { it.hasNext() }

  override fun next(): List<T> = iterators.map {
    if (it.hasNext()) it.next() else defaultValue
  }.also { ++count }
}

private class TotalSizeLimitingIterator(
  private val limit: Int,
  private val delegate: CompoundIterator<String>,
) : Iterator<List<String>> {
  private var size = 0L
  private var cached = emptyList<String>()
  private var cachedSize = 0

  init {
    if (delegate.hasNext()) {
      cached = delegate.next()
      cachedSize = cached.sumOf { it.length }
    }
    else {
      cachedSize = -1
    }
  }

  override fun hasNext(): Boolean = cachedSize > -1

  override fun next(): List<String> {
    val result = cached

    if (!delegate.hasNext()) {
      cachedSize = -1
    }
    else {
      cached = delegate.next()
      cachedSize = cached.sumOf { it.length }
      size += cachedSize
      if (size > limit) cachedSize = -1
    }

    return result
  }
}

private class LimitedIterator<T>(
  private val delegate: Iterator<T>,
  private val limit: Int,
) : Iterator<T> {
  private var current = 0

  override fun hasNext(): Boolean = delegate.hasNext() && current < limit

  override fun next(): T {
    if (delegate.hasNext()) current += 1
    return delegate.next()
  }
}
