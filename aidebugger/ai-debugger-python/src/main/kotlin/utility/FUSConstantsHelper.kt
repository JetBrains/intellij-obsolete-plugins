package com.intellij.aidebugger.python.utility

import com.intellij.aidebugger.python.extensions.RunnerCommon
import com.intellij.aidebugger.common.SkipReason as FusSkip

// No else branch to keep it exhaustive: to add a new enum value also update the FUS skip reason and bump FUS version!
fun RunnerCommon.SkipReason.toFusString(): String = when (this) {
    RunnerCommon.SkipReason.NOT_SUPPORTED_EXECUTOR -> FusSkip.NOT_SUPPORTED_EXECUTOR
    RunnerCommon.SkipReason.DISABLED -> FusSkip.DISABLED
    RunnerCommon.SkipReason.RUN_CONFIG_IS_NULL -> FusSkip.RUN_CONFIG_IS_NULL
    RunnerCommon.SkipReason.SDK_IS_NULL -> FusSkip.SDK_IS_NULL
    RunnerCommon.SkipReason.PYTHON_VERSION_IS_NULL -> FusSkip.PYTHON_VERSION_IS_NULL
    RunnerCommon.SkipReason.PYTHON_VERSION_IS_INVALID -> FusSkip.PYTHON_VERSION_IS_INVALID
    RunnerCommon.SkipReason.LOW_PYTHON_VERSION -> FusSkip.LOW_PYTHON_VERSION
    RunnerCommon.SkipReason.REMOTE_INTERPRETER -> FusSkip.NON_AI_PROJECT
}
