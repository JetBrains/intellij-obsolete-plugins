package com.intellij.aidebugger.evaluation.models.entities

data class InputSpec(
    val id: String,
    val input: String,
    val expectedOutput: String? = null,
    val inputStruct: String? = null
)