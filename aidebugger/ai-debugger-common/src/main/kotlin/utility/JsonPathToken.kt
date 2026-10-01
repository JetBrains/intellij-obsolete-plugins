package com.intellij.aidebugger.common.utility

sealed interface JsonPathToken {
    data class Segment(val name: String) : JsonPathToken
    data class Idx(val i: Int) : JsonPathToken
}