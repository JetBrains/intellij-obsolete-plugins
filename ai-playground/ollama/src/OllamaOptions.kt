package com.intellij.aiplayground.ollama

data class OllamaOptions(
  val numPredict: Int? = null,
  val temperature: Double? = null,
  val topP: Double? = null,
  val contextSize: Int? = null
) {
  operator fun plus(rhs: OllamaOptions) = OllamaOptions(
    contextSize = rhs.contextSize ?: contextSize
  )
}