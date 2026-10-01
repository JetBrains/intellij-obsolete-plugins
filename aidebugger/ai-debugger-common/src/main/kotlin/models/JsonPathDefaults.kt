package com.intellij.aidebugger.common.models

/**
 * Default JSON paths for extracting input/output from trace events.
 * These are used as fallbacks when no custom paths are configured.
 */
object JsonPathDefaults {
    const val DEFAULT_INPUT_PATH = "$.rootEvents[-1].children[-1].payload.inputs.messages[0].content"
    const val DEFAULT_OUTPUT_PATH = "$.rootEvents[-1].children[-1].payload.outputs.messages[-1].content"
}