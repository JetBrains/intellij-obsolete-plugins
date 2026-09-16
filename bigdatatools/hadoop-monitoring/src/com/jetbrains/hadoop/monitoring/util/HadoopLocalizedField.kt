package com.jetbrains.hadoop.monitoring.util

import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.bigdatatools.common.monitoring.table.extension.LocalizedField
import kotlin.reflect.KProperty1

class HadoopLocalizedField<T : RemoteInfo>(field: KProperty1<T, *>, i18Key: String?) : LocalizedField<T>(field, i18Key) {
  override fun getLocalizedName() = i18Key?.let { HadoopMessagesBundle.message(it) } ?: ""
}