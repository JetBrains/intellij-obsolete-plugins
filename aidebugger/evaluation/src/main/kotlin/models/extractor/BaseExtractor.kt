package com.intellij.aidebugger.evaluation.models.extractor

import com.intellij.aidebugger.evaluation.models.entities.DataPoint

/**
 * Extracts a DataPoint view from raw debugger trace payloads.
 * The extractor may rely on a combination of raw payload fields and fallback values.
 */
interface BaseExtractor {

    /**
     * @param raw             Raw trace payload from ai-debugger
     * @param fallback        Fallback values derived from the request context (including experimentId)
     */
    fun extract(
        raw: Map<String, Any?>,
        fallback: FallbackContext
    ): DataPoint
}

data class FallbackContext(
    val id: String,
    val input: String,
    val outputExpected: String?,
    val experimentId: String? = null
)
