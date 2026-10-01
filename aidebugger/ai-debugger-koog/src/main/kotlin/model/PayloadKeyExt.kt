package com.intellij.aidebugger.koog.model

import com.intellij.aidebugger.common.models.entities.PayloadKey

/**
 * Represents a key associated with the tool in a payload.
 */
val PayloadKey.Tool
    get() = "tool"

/**
 * Represents a key associated with the tool arguments in a payload.
 */
val PayloadKey.ToolArguments
    get() = "arguments"