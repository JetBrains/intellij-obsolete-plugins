package com.intellij.aidebugger.common.models

data class DatasetInfo(
    var name: String,
    var fileName: String,
    var itemsCount: Int = 0,
    var lastModified: Long = 0
)