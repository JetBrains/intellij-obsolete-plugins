package com.intellij.bigdatatools.zeppelin.ztools.controller.model

internal class ZtoolsDebugInfo(var sourceCodes: List<String> = emptyList(),
                               val collectedOutputs: MutableList<Any> = mutableListOf(),
                               var totalOutput: Any? = null)