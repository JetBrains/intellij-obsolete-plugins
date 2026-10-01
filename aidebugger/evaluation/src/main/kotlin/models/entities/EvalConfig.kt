package com.intellij.aidebugger.evaluation.models.entities

// Simple table row models used by Evaluation view
data class ConfigRow(
    val name: String,
    val dataset: String,
    val items: String,
    val avgAgentTimeSec: String,
    val avgAgentTokens: String,
    val latestRun: String,
    val aggregatedScores: Map<String, String> = emptyMap()
)

data class ConfigEvalRow(
    var id: String,
    var input: String,
    var output: String,
    var evaluatorScores: Map<String, String>,
    var evaluatorExtras: Map<String, String>
)
