package com.intellij.dataWrangler.executor

import com.intellij.dataWrangler.operations.DataWranglerCommand
import com.intellij.dataWrangler.operations.TransformationStep
import kotlinx.coroutines.flow.Flow

interface DataWranglerTransformationStepsManager<C : DataWranglerContext> {

  fun removeTransformation(step: TransformationStep<*, *>)

  fun addTransformation(step: TransformationStep<*, C>)

  fun getTransformationsList(): List<TransformationStep<*, C>>

  fun resetTransformationsTo(step: TransformationStep<*, *>)

  fun findIndexByStep(step: TransformationStep<*, *>): Int?

  fun getEventFlow(): Flow<List<TransformationStep<*, out DataWranglerContext>>>
  fun getErrorsFlow(): Flow<Throwable>

  fun getExecutedCommands(): List<DataWranglerCommand<C>>

  fun reportState(step: TransformationStep<*, C>, state: StepState)

  sealed interface StepState {
    object NotRun: StepState
    object Running: StepState
    object Ok: StepState
    object Cancelled: StepState
    class Failed(val th: Throwable): StepState
  }
}