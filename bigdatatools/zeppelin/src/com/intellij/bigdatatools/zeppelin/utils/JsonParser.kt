package com.intellij.bigdatatools.zeppelin.utils

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.intellij.bigdatatools.coreUi.serializer.BdtJson

/**
 * Object, which implements methods for work with Json
 */
object JsonParser {
  fun toJson(value: Any): String = BdtJson.toJson(value)

  @Suppress("UNCHECKED_CAST")
  fun fromJsonToMap(value: String): Map<String, Any?> = BdtJson.fromJsonToMapStringAny(value)

  fun <T> fromValueObject(value: Any?, clazz: Class<T>): T = BdtJson.fromValueObjToClass(value, clazz)

  fun <T> parseStringJsonToObject(jsonString: String, clazz: Class<T>): T = BdtJson.fromJsonToClass(jsonString, clazz)

  fun <T> fromStringObject(json: String, clazz: Class<T>): T = BdtJson.fromJsonToClass(json, clazz)

  fun <T> fromValueList(value: Any, clazz: Class<T>): List<T> = BdtJson.fromValueObjListToClassList(value, clazz)

  fun <T> fromValueMap(value: Any, clazz: Class<T>): Map<String, T?> = BdtJson.fromValueObjToClassMap(value, clazz)

  fun fromJsonObjectToMap(jsonObj: JsonObject): Map<String, Any?> =
    Gson().fromJson<HashMap<String, Any?>>(jsonObj.toString(), object : TypeToken<HashMap<String, Any?>>() {}.type) ?: emptyMap()

  fun fromObjectToMap(obj: Any): Map<String, Any?> = BdtJson.fromClassToMap(obj)
}

//@Suppress("unused")
//private class InstantiationTypeAdapter {
//  @FromJson
//  fun fromJson(jsonReader: JsonReader, delegate: JsonAdapter<InstantiationType>): InstantiationType? {
//    val value = jsonReader.nextString()
//    return try {
//      delegate.fromJsonValue(value)
//    }
//    catch (e: Exception) {
//      InstantiationType.SHARED
//    }
//  }
//}