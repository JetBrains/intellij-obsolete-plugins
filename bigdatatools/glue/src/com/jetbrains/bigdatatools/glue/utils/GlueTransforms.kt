package com.jetbrains.bigdatatools.glue.utils

import com.jetbrains.bigdatatools.common.monitoring.data.model.FieldGroupsData
import com.jetbrains.bigdatatools.common.monitoring.data.model.FieldsDataModel
import com.jetbrains.bigdatatools.common.util.TimeUtils
import software.amazon.awssdk.services.glue.model.GetSchemaResponse
import software.amazon.awssdk.services.glue.model.SerDeInfo
import software.amazon.awssdk.services.glue.model.StorageDescriptor
import software.amazon.awssdk.services.glue.model.Table

object GlueTransforms {
  fun getSchemaInfoDetails(schema: GetSchemaResponse): FieldsDataModel {
    val fileds = listOf(
      GlueMessagesBundle.message("schema.info.name") to schema.schemaName(),
      GlueMessagesBundle.message("schema.info.arn") to schema.schemaArn(),
      GlueMessagesBundle.message("schema.info.registry") to schema.registryName(),
      GlueMessagesBundle.message("schema.info.format") to schema.dataFormatAsString(),
      GlueMessagesBundle.message("schema.info.compability") to schema.compatibilityAsString(),
      GlueMessagesBundle.message("schema.info.description") to (schema.description()?.ifBlank { null } ?: "-"),
      GlueMessagesBundle.message("schema.info.last.updated") to schema.updatedTime(),
      GlueMessagesBundle.message("schema.info.version") to schema.latestSchemaVersion(),
    ).filter { it.second != null }
    return FieldsDataModel.createForList(fileds)
  }


  fun getTableLocalizedFields(info: Table) = FieldGroupsData(info, listOf(
    GlueMessagesBundle.message("table.info.common.title") to FieldsDataModel.createForList(commonTableInfo(info)),
    GlueMessagesBundle.message("table.info.params.title") to FieldsDataModel.createForList(paramsInfo(info)),
    GlueMessagesBundle.message("meta.separator.storage.description") to FieldsDataModel.createForList(
      storageInfo(info.storageDescriptor())),
    GlueMessagesBundle.message("meta.separator.serializer.deserializer") to FieldsDataModel.createForList(
      serializerInfo(info.storageDescriptor().serdeInfo())),
  ))

  private fun commonTableInfo(info: Table) = listOfNotNull(
    GlueMessagesBundle.message("meta.label.type") to info.tableType(),
    GlueMessagesBundle.message("meta.label.owner") to info.owner(),
    if (info.createTime() != null && info.createTime().toEpochMilli() > 0) {
      GlueMessagesBundle.message("meta.label.create.time") to TimeUtils.unixTimeToString(info.createTime().toEpochMilli())
    }
    else null,
    if (info.lastAccessTime() != null && info.lastAccessTime().toEpochMilli() > 0) {
      GlueMessagesBundle.message("meta.label.last.access.time") to
        TimeUtils.unixTimeToString(info.lastAccessTime().toEpochMilli())
    }
    else
      null,
    GlueMessagesBundle.message("meta.label.retention") to info.retention().toString(),
  )

  private fun paramsInfo(table: Table) = table.parameters().entries.map { it.key to it.value }.filtrated()

  private fun storageInfo(sd: StorageDescriptor) = listOfNotNull(
    GlueMessagesBundle.message("meta.label.input.format") to sd.inputFormat(),
    GlueMessagesBundle.message("meta.label.output.format") to sd.outputFormat(),
    GlueMessagesBundle.message("meta.label.location") to sd.location(),
    GlueMessagesBundle.message("meta.label.cols.size") to sd.columns().size.toString(),
    GlueMessagesBundle.message("meta.label.compressed") to sd.compressed().toString(),
    GlueMessagesBundle.message("meta.label.sort.columns") to sd.hasSortColumns().toString(),
    if (sd.numberOfBuckets() >= 0) {
      GlueMessagesBundle.message("meta.label.num.buckets") to sd.numberOfBuckets().toString()
    }
    else null
  ).filtrated()


  private fun serializerInfo(serdeInfo: SerDeInfo) = listOfNotNull(
    GlueMessagesBundle.message("meta.label.name") to serdeInfo.name(),
    GlueMessagesBundle.message("meta.label.library") to serdeInfo.serializationLibrary(),
  ).filtrated()

  @Suppress("UNCHECKED_CAST")
  private fun List<Pair<String, String?>>.filtrated(): List<Pair<String, String>> =
    filter { it.second != null && it.second?.isNotBlank() == true } as List<Pair<String, String>>
}