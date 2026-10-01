// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.python.huggingFace.modelChoice.modelHandling

import com.intellij.openapi.project.Project
import com.intellij.python.community.impl.huggingFace.api.HuggingFaceEntityBasicApiData
import com.intellij.python.community.impl.huggingFace.cache.HuggingFaceModelsCache
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
class HfModelInserter(private val project: Project, private val onEnded: () -> Unit) {
  fun insertModel(model: HuggingFaceEntityBasicApiData?) {
    if (model == null) return  // must be unreachable

    val libraryName = model.libraryName?.let { HfModelChoiceSupportedLibraries.fromName(it) }
    val itemId = model.itemId
    val pipelineTag = model.pipelineTag

    val handler = when(libraryName) {
      HfModelChoiceSupportedLibraries.TRANSFORMERS -> HfTransformersHandler()
      HfModelChoiceSupportedLibraries.DIFFUSERS -> HfDiffusersHandler()
      HfModelChoiceSupportedLibraries.STABLE_BASELINES3 -> HfStableBaseline3Handler()
      HfModelChoiceSupportedLibraries.FAIRSEQ -> HfFairseqHandler()
      HfModelChoiceSupportedLibraries.ESPNET -> HfEspnetHandler()
      HfModelChoiceSupportedLibraries.PYANNOTE -> HfPyannoteHandler()
      HfModelChoiceSupportedLibraries.SENTENCE_TRANSFORMERS -> HfSentenceTransformersHandler()
      null -> null
    }

    if (handler == null) return  // must be unreachable

    handler.insertCode(itemId, pipelineTag, project)
    HuggingFaceModelsCache.saveEntity(model)
    onEnded()
  }
}
