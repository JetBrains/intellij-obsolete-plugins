package com.jetbrains.hadoop.monitoring.rest.resourcemanager

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.bigdatatools.coreUi.serializer.JsonDeserializeException
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.CapacitySchedulerInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.FairSchedulerInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.FifoSchedulerInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.ResourceInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.SchedulerInfo
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory


object RmParser {
  private val gson = Gson()
  private val moshi = Moshi.Builder()
    .add(PolymorphicJsonAdapterFactory.of(SchedulerInfo::class.java, "type")
           .withSubtype(FifoSchedulerInfo::class.java, SchedulerInfo.Type.fifoScheduler.name)
           .withSubtype(CapacitySchedulerInfo::class.java, SchedulerInfo.Type.capacityScheduler.name)
           .withSubtype(FairSchedulerInfo::class.java, SchedulerInfo.Type.fairScheduler.name))
    .add(KotlinJsonAdapterFactory())
    .build()

  fun <T> parseClassMap(jsonString: String, clazz: Class<T>): Map<String, T?> {
    val mapObj = moshi.adapter(Map::class.java).fromJson(jsonString)
                 ?: throw JsonDeserializeException(jsonString, Map::class.toString())

    return mapObj.entries.map { (k, v) -> k.toString() to fromValueObjToClass(v, clazz) }.toMap()
  }

  fun <T> parseClassMapRemoveDublicatesMap(jsonString: String, clazz: Class<T>): Map<String, T?> {
    val gsonJson = gson.fromJson(jsonString, JsonObject::class.java)
    return parseClassMap(gson.toJson(gsonJson), clazz)
  }


  fun parseResourceInfoFromString(resourceString: String): ResourceInfo {
    val trimmed = resourceString.removePrefix("<memory:").removeSuffix(">")
    val memory = trimmed.split(",").first().toLong()
    val vCores = trimmed.split(":").last().toInt()
    return ResourceInfo(memory, vCores)

  }
  private fun <T> fromValueObjToClass(value: Any?, clazz: Class<T>): T? =
    moshi.adapter(clazz).fromJsonValue(value)

}