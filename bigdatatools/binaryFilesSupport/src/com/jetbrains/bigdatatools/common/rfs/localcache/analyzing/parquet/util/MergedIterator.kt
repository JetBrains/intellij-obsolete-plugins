package com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.parquet.util

class MergedIterator(private val iterator1: Iterator<String>,
                     private val iterator2: Iterator<String>) : Iterator<String> {
  override fun hasNext(): Boolean {
    return iterator1.hasNext() && iterator2.hasNext()
  }

  override fun next(): String {
    val column1 = iterator1.next().trimColumn()
    val column2 = iterator2.next().trimColumn()
    return column1.zip(column2).joinToString(prefix = "[", postfix = "]") { "(${it.first}, ${it.second})" }
  }

  private fun String.trimColumn(): List<String> = this.trim('[', ']').split(", ")
}