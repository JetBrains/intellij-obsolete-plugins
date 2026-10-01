// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.python.huggingFace.tags

import com.intellij.python.huggingFace.HuggingFaceProBundle
import com.intellij.python.huggingFace.icons.PythonHuggingFaceIcons
import org.jetbrains.annotations.Nls
import javax.swing.Icon

object HfPipelineTagMappings {
  private val DEFAULT_ICON = PythonHuggingFaceIcons.GraphMachineLearning

  // The API response does not include display names.
  // Given that these categories are unlikely to undergo frequent changes,
  // it is acceptable to hardcode them in this instance.
  private val subtypeDisplayNameMap = mapOf(
    "audio" to "Audio",
    "rl" to "Reinforcement Learning",
    "cv" to "Computer Vision",
    "tabular" to "Tabular",
    "multimodal" to "Multimodal",
    "nlp" to "Natural Language Processing",
    "other" to "Other"
  )

  private val tagIconMap = mapOf(
    "audio-classification" to PythonHuggingFaceIcons.AudioClassification,
    "audio-to-audio" to PythonHuggingFaceIcons.AudioToAudio,
    "automatic-speech-recognition" to PythonHuggingFaceIcons.AutomaticSpeechRecognition,
    "depth-estimation" to PythonHuggingFaceIcons.DeepEstimation,
    "document-question-answering" to PythonHuggingFaceIcons.DocumentQuestionAnsering,
    "feature-extraction" to PythonHuggingFaceIcons.FeatureExtraction,
    "fill-mask" to PythonHuggingFaceIcons.FillMask,
    "graph-ml" to PythonHuggingFaceIcons.GraphMachineLearning,
    "image-classification" to PythonHuggingFaceIcons.ImageClassification,
    "image-segmentation" to PythonHuggingFaceIcons.ImageSegmentation,
    "image-to-3d" to PythonHuggingFaceIcons.ImageTo3D,
    "image-to-image" to PythonHuggingFaceIcons.ImageToImage,
    "image-to-text" to PythonHuggingFaceIcons.ImageToText,
    "image-text-to-text" to PythonHuggingFaceIcons.ImageTextToText,
    "image-to-video" to PythonHuggingFaceIcons.ImageToVideo,
    "mask-generation" to PythonHuggingFaceIcons.MaskGeneration,
    "object-detection" to PythonHuggingFaceIcons.ObjectDetection,
    "question-answering" to PythonHuggingFaceIcons.QuestionAnswering,
    "reinforcement-learning" to PythonHuggingFaceIcons.ReinforcementLearning,
    "robotics" to PythonHuggingFaceIcons.Robotics,
    "sentence-similarity" to PythonHuggingFaceIcons.SentenceSimilarity,
    "summarization" to PythonHuggingFaceIcons.Summarization,
    "table-question-answering" to PythonHuggingFaceIcons.TableQuestionAnswering,
    "tabular-classification" to PythonHuggingFaceIcons.TabularClassification,
    "tabular-regression" to PythonHuggingFaceIcons.TabularRegression,
    "text-classification" to PythonHuggingFaceIcons.TextClassification,
    "text-generation" to PythonHuggingFaceIcons.TextGeneration,
    "text-to-3d" to PythonHuggingFaceIcons.TextTo3D,
    "text-to-audio" to PythonHuggingFaceIcons.TextToAudio,
    "text-to-image" to PythonHuggingFaceIcons.TextToImage,
    "text-to-speech" to PythonHuggingFaceIcons.TextToSpeech,
    "text-to-video" to PythonHuggingFaceIcons.TextToVideo,
    "text2text-generation" to PythonHuggingFaceIcons.TextToTextGeneration,
    "token-classification" to PythonHuggingFaceIcons.TokenClassification,
    "translation" to PythonHuggingFaceIcons.Translation,
    "unconditional-image-generation" to PythonHuggingFaceIcons.UnconditionalImageGeneration,
    "video-classification" to PythonHuggingFaceIcons.VideoClassification,
    "visual-question-answering" to PythonHuggingFaceIcons.VisualQuestionAnswering,
    "voice-activity-detection" to PythonHuggingFaceIcons.VoiceActivityDetection,
    "zero-shot-classification" to PythonHuggingFaceIcons.ZeroShotClassification,
    "zero-shot-image-classification" to PythonHuggingFaceIcons.ZeroShotImageClassification,
    "zero-shot-object-detection" to PythonHuggingFaceIcons.ZeroShotObjectDetection,
  )

  @Nls fun getSubtypeDisplayNameMap(key: String): String =
    subtypeDisplayNameMap.getOrDefault(key, HuggingFaceProBundle.message("python.hugging.face.model.default.model.subtype"))
  fun getPipelineTagIcon(key: String): Icon = tagIconMap.getOrDefault(key, DEFAULT_ICON)
}
