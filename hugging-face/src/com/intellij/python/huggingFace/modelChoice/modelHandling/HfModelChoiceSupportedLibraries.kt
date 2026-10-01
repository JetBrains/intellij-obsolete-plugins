package com.intellij.python.huggingFace.modelChoice.modelHandling

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
@Suppress("SpellCheckingInspection")
enum class HfModelChoiceSupportedLibraries(val libraryName: String) {
  // All supported Hugging Face libraries can be found here:
  // https://huggingface.co/docs/hub/models-libraries
  DIFFUSERS("diffusers"),
  TRANSFORMERS("transformers"),
  SENTENCE_TRANSFORMERS("sentence-transformers"),
  FAIRSEQ("fairseq"),
  ESPNET("espnet"),
  PYANNOTE("pyannote-audio"),
  STABLE_BASELINES3("stable-baslines3");
  // https://huggingface.co/speechbrain/metricgan-plus-voicebank?library=true -> speechbrain.pretrained

  companion object {
    fun fromName(name: String): HfModelChoiceSupportedLibraries? = entries.find { it.libraryName == name }
  }
}
