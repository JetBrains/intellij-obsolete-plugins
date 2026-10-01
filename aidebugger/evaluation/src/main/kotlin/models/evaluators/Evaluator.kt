package com.intellij.aidebugger.evaluation.models.evaluators

import com.intellij.aidebugger.evaluation.models.entities.DataPoint
import com.intellij.aidebugger.evaluation.models.entities.EvalResult

/** Base evaluator contract (ported from python/engine/base.py). */
interface Evaluator {
    suspend fun evaluate(dataPoint: DataPoint): EvalResult
}
