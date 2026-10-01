package com.intellij.aidebugger.common.models.entities

data class EventStackFrame(
    val filePath: String,
    val functionName: String,
    val lineNumber: Int
)