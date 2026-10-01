package com.intellij.aidebugger.evaluation.models.entities

import com.fasterxml.jackson.annotation.JsonCreator

data class DataPoint @JsonCreator constructor(
    val id: String,
    val input: String,
    val outputGen: String,
    val outputExpected: String,
    val experimentId: String,
    val raw: Map<String, Any?> = emptyMap(),
    val runDatetime: String? = null,
    val exception: String? = null,
)
