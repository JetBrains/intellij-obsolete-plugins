// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.python.huggingFace.tags

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.intellij.python.huggingFace.HuggingFaceProBundle
import org.jetbrains.annotations.ApiStatus


@ApiStatus.Internal
class HfTagLoader {
  private val mapper = ObjectMapper().registerKotlinModule()

  private val rootNode by lazy {
    synchronized(this) {
      // todo: maybe some persistent cache that updates once in 1-2-3 days
      // endpoint: GET https://huggingface.co/api/models-tags-by-type?
      val resourceStream = this::class.java.classLoader.getResourceAsStream(TAGS_FILE_JSON)
                           ?: throw IllegalArgumentException(HuggingFaceProBundle.message("python.hugging.face.tags.json.not.found"))
      resourceStream.use {
        mapper.readTree(it)
      }
    }
  }

  fun loadPipelineTags(): Map<String, List<HfPipelineTag>> {
    val tagsArray = rootNode.get(HfPipelineTag.PIPELINE_TAG)
    val tags: List<HfPipelineTag> = mapper.readValue(tagsArray.toString())

    return tags.groupBy { it.subType }
  }

  fun loadLicenseTags(): List<HfTag> {
    val tagsArray = rootNode.get(LICENSE_TAG)
    return mapper.readValue(tagsArray.toString())
  }

  fun loadOtherTags(): List<HfTag> {
    val tagsArray = rootNode.get(DATASET_TAG) + rootNode.get(OTHER_TAG)
    return mapper.readValue(tagsArray.toString())
  }

  companion object {
    const val TAGS_FILE_JSON: String = "huggingFace/HfTagsInfo.json"
    const val LICENSE_TAG: String = "license"
    const val DATASET_TAG: String = "dataset"
    const val OTHER_TAG: String = "other"
  }
}
