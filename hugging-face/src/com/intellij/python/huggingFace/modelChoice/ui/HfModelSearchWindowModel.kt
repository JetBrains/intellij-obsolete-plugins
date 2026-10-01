// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.python.huggingFace.modelChoice.ui

import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.python.community.impl.huggingFace.api.HuggingFaceEntityBasicApiData
import com.intellij.python.community.impl.huggingFace.api.HuggingFaceModelSortKey
import com.intellij.python.huggingFace.modelChoice.modelHandling.HfModelValidityChecker
import com.intellij.python.huggingFace.tags.HfPipelineTag
import com.intellij.python.huggingFace.tags.HfTag
import kotlin.properties.Delegates

internal data class HfModelSearchWindowModel(
  var searchFieldString: String? = null,
  var sortKey: HuggingFaceModelSortKey? = HuggingFaceModelSortKey.LIKES,
  var pipelineTag: HfPipelineTag? = null,
  var language: HfTag? = null,
  var license: HfTag? = null,
  var otherTags: HfTag? = null
) {
  private val validityChecker = HfModelValidityChecker()

  fun createTagString(): String {
    return listOfNotNull(license?.id,
                         otherTags?.id,
                         pipelineTag?.id
    ).joinToString(",")
  }

  var selectedModel: HuggingFaceEntityBasicApiData? by Delegates.observable(null) {
    _, _, new ->
    val isSuitable = new?.let { validityChecker.isModelSuitable(it) } ?: false
    isInsertPossible.set(isSuitable)
  }

  val isInsertPossible = AtomicProperty(false)
}
