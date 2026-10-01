package com.intellij.python.huggingFace.modelChoice.modelHandling

import com.intellij.python.community.impl.huggingFace.api.HuggingFaceEntityBasicApiData
import org.jetbrains.annotations.ApiStatus


@ApiStatus.Internal
class HfModelValidityChecker {
  fun isModelSuitable(model: HuggingFaceEntityBasicApiData): Boolean {
    val library = model.libraryName?.let { HfModelChoiceSupportedLibraries.fromName(it) }

    return when(library) {
      HfModelChoiceSupportedLibraries.TRANSFORMERS -> AVAILABLE_TRANSFORMERS_TASKS.contains(model.pipelineTag)
      HfModelChoiceSupportedLibraries.DIFFUSERS -> true
      HfModelChoiceSupportedLibraries.STABLE_BASELINES3 -> true
      HfModelChoiceSupportedLibraries.FAIRSEQ -> true
      HfModelChoiceSupportedLibraries.ESPNET -> true
      HfModelChoiceSupportedLibraries.PYANNOTE -> true
      HfModelChoiceSupportedLibraries.SENTENCE_TRANSFORMERS -> true
      null -> false
    }
  }

  companion object {

    private val AVAILABLE_TRANSFORMERS_TASKS: Set<String> = setOf(
      "audio-classification",
      "automatic-speech-recognition",
      "conversational",
      "depth-estimation",
      "document-question-answering",
      "feature-extraction",
      "fill-mask",
      "image-classification",
      "image-feature-extraction",
      "image-segmentation",
      "image-to-image",
      "image-to-text",
      "mask-generation",
      "ner",
      "object-detection",
      "question-answering",
      "sentiment-analysis",
      "summarization",
      "table-question-answering",
      "text-classification",
      "text-generation",
      "text-to-audio",
      "text-to-speech",
      "text2text-generation",
      "token-classification",
      "translation",
      "video-classification",
      "visual-question-answering",
      "vqa",
      "zero-shot-audio-classification",
      "zero-shot-classification",
      "zero-shot-image-classification",
      "zero-shot-object-detection",
      "translation_XX_to_YY"
    )
  }
}
