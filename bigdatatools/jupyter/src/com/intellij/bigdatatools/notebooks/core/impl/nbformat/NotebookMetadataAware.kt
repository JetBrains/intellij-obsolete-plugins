// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bigdatatools.notebooks.core.impl.nbformat

import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * Represents object holding set of named [JsonElement]'s as a metadata.
 */
interface NotebookMetadataAware {
  fun getKeys(): Set<String>
  fun setMetadata(key: String, value: JsonElement)
  fun getMetadata(key: String): JsonElement?
  fun removeMetadata(key: String)
}

abstract class NotebookMetadataAwareBase : NotebookMetadataAware {
  /**
   * Represents bare object content including objects metadata.
   */
  abstract val json: JsonObject

  fun getOrCreateMetadata(): JsonObject = getMetadata() ?: createMetadata()

  /**
   * Retrieve [JsonObject] representing metadata from object [json].
   * @return [JsonObject] if metadata node is present, `null` otherwise
   *
   * @see createMetadata
   */
  open fun getMetadata(): JsonObject? = json["metadata"]?.asJsonObject?.get("bdt")?.asJsonObject

  /**
   * Create [JsonObject] representing object metadata and attach it to the object [json].
   *
   * @see getOrCreateMetadata
   */
  open fun createMetadata(): JsonObject {
    if (json.has("metadata")) {
      json["metadata"].asJsonObject.add("bdt", JsonObject())
    }
    else {
      json.add("metadata", JsonObject().apply {
        add("bdt", JsonObject())
      })
    }
    return json["metadata"].asJsonObject["bdt"].asJsonObject
  }

  override fun getKeys(): Set<String> = getMetadata()?.keySet() ?: emptySet()

  override fun getMetadata(key: String): JsonElement? = getMetadata()?.get(key)

  override fun removeMetadata(key: String) {
    getMetadata()?.remove(key)
  }

  override fun setMetadata(key: String, value: JsonElement) = getOrCreateMetadata().add(key, value)
}