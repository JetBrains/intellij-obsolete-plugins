package com.jetbrains.spark.monitoring.graph.model

import com.intellij.openapi.util.NlsSafe
import com.jetbrains.spark.monitoring.graph.view.DotNodeGroup

data class DotItem(val id: String, @NlsSafe val name: String, val group: DotNodeGroup)