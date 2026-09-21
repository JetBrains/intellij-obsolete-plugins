package com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.parquet

/**
 * User: Dmitry.Naydanov
 * Date: 2018-11-20.
 */
data class DownloadDescriptor(val index: Int, val offset: Long, val size: Int, val append: Boolean = false)