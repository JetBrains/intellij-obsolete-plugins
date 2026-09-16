package com.intellij.bigdatatools.zeppelin.statistics

import com.intellij.bigdatatools.zeppelin.components.ZeppelinInstanceCachedConnection
import com.intellij.bigdatatools.zeppelin.components.containers.service.ZeppelinNoteCacheConnection
import com.intellij.bigdatatools.zeppelin.models.connection.ZeppelinInfo
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.openapi.util.Version

val zeppelin_version = EventFields.StringValidatedByRegexpReference("zeppelin_version", "version")

internal fun ZeppelinInstanceCachedConnection.getContext() = UsageContext(zeppelinInfo)

internal fun ZeppelinNoteCacheConnection.getContext() = UsageContext(zeppelinInfo)

data class UsageContext(val zeppelinVersion: Version?) {
  constructor(zeppelinInfo: ZeppelinInfo?) : this(if (zeppelinInfo?.version == null) null else Version.parseVersion(zeppelinInfo.version))
}

fun Version?.toStatisticsString() = if (this != null) "${major}.${minor}" else "unknown.format"