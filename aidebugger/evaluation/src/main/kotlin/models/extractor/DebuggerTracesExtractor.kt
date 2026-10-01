package com.intellij.aidebugger.evaluation.models.extractor

import com.google.gson.Gson
import com.intellij.aidebugger.common.utility.JsonPathUtils
import com.intellij.aidebugger.evaluation.models.entities.DataPoint

/**
 * A tolerant extractor for ai-debugger raw traces.
 * It tries to map common fields:
 * - input       -> ["input"], ["request", "input"], ["prompt"], else fallback.input
 * - outputGen  -> ["outputGen"], ["response", "text"], ["result"], ["final_output"], else from top-level "output" if present
 * - id          -> ["id"], else fallback.id
 * - outputExpected -> fallback.outputExpected only (we keep user's prior expectation)
 */
class DebuggerTracesExtractor(private val outputPath: String) : BaseExtractor {

    private val _gson = Gson()

    override fun extract(
        raw: Map<String, Any?>,
        fallback: FallbackContext
    ): DataPoint {
        val rootJson = _gson.toJsonTree(raw)

        val exTokens = JsonPathUtils.parseJsonPath(outputPath)
        val exEl = JsonPathUtils.getValueByPath(rootJson, exTokens)
        val outputGen = JsonPathUtils.jsonStringLike(exEl) ?: ""

        return DataPoint(
            id = fallback.id,
            input = fallback.input,
            outputGen = outputGen,
            outputExpected = fallback.outputExpected ?: "",
            experimentId = fallback.experimentId?: "",
            raw = raw
        )
    }
}
