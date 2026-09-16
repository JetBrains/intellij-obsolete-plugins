package com.jetbrains.bigdatatools.hivemetastore.utils

import com.jetbrains.bigdatatools.common.monitoring.data.model.FieldGroupsData
import com.jetbrains.bigdatatools.common.monitoring.data.model.FieldsDataModel
import com.jetbrains.bigdatatools.common.util.TimeUtils
import com.jetbrains.bigdatatools.hivemetastore.monitoring.models.HiveTableInfo
import org.apache.hadoop.hive.metastore.api.SerDeInfo
import org.apache.hadoop.hive.metastore.api.StorageDescriptor
import org.apache.hadoop.hive.metastore.api.Table

object HiveTransforms {
  fun getTableLocalizedFields(info: HiveTableInfo) = FieldGroupsData(info, listOf(
    HiveMessagesBundle.message("table.info.common.title") to FieldsDataModel.createForList(commonTableInfo(info.origin)),
    HiveMessagesBundle.message("table.info.params.title") to FieldsDataModel.createForList(paramsInfo(info.origin)),
    HiveMessagesBundle.message("meta.separator.storage.description") to FieldsDataModel.createForList(storageInfo(info.origin.sd)),
    HiveMessagesBundle.message("meta.separator.serializer.deserializer") to FieldsDataModel.createForList(
      serializerInfo(info.origin.sd.serdeInfo)),
  ))

  private fun commonTableInfo(info: Table) = listOfNotNull(
    HiveMessagesBundle.message("meta.label.type") to info.tableType,
    HiveMessagesBundle.message("meta.label.owner") to info.owner,
    HiveMessagesBundle.message("meta.label.owner.type") to info.ownerType.name,
    if (info.createTime > 0) {
      HiveMessagesBundle.message("meta.label.create.time") to TimeUtils.unixTimeToString(info.createTime.toLong() * 1000)
    }
    else null,
    if (info.lastAccessTime > 0) {
      HiveMessagesBundle.message("meta.label.last.access.time") to
        TimeUtils.unixTimeToString(info.lastAccessTime.toLong() * 1000)
    }
    else
      null,
    HiveMessagesBundle.message("meta.label.temporary") to info.isTemporary.toString(),
    HiveMessagesBundle.message("meta.label.retention") to info.retention.toString(),
  )

  private fun paramsInfo(table: Table) = table.parameters.entries.map { it.key to it.value }.filtrated()

  private fun storageInfo(sd: StorageDescriptor) = listOfNotNull(
    HiveMessagesBundle.message("meta.label.input.format") to sd.inputFormat,
    HiveMessagesBundle.message("meta.label.output.format") to sd.outputFormat,
    HiveMessagesBundle.message("meta.label.location") to sd.location,
    HiveMessagesBundle.message("meta.label.cols.size") to sd.bucketColsSize.toString(),
    HiveMessagesBundle.message("meta.label.compressed") to sd.isCompressed.toString(),
    HiveMessagesBundle.message("meta.label.sort.columns") to sd.sortCols.toString(),
    if (sd.numBuckets >= 0) {
      HiveMessagesBundle.message("meta.label.num.buckets") to sd.numBuckets.toString()
    }
    else null
  ).filtrated()


  private fun serializerInfo(serdeInfo: SerDeInfo) = listOfNotNull(
    HiveMessagesBundle.message("meta.label.name") to serdeInfo.name,
    HiveMessagesBundle.message("meta.label.type") to serdeInfo.serdeType?.name,
    HiveMessagesBundle.message("meta.label.description") to serdeInfo.description,
    HiveMessagesBundle.message("meta.label.library") to serdeInfo.serializationLib,
    HiveMessagesBundle.message("meta.label.serializer") to serdeInfo.serializerClass,
    HiveMessagesBundle.message("meta.label.deserializer") to serdeInfo.deserializerClass,
  ).filtrated()

  @Suppress("UNCHECKED_CAST")
  private fun List<Pair<String, String?>>.filtrated(): List<Pair<String, String>> =
    filter { it.second != null && it.second?.isNotBlank() == true } as List<Pair<String, String>>
}