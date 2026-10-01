package com.intellij.dataWrangler.impl.operations

import com.intellij.dataWrangler.executor.DataWranglerContext
import com.intellij.dataWrangler.executor.DataWranglerTransformationStepsManager
import com.intellij.dataWrangler.executor.DataWranglerTransformationStepsManager.StepState
import com.intellij.dataWrangler.impl.fus.DataWranglerProviderCollector
import com.intellij.dataWrangler.operations.DataWranglerCommand
import com.intellij.dataWrangler.operations.TransformationStep
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.Collections.synchronizedList
import java.util.LinkedList

class DataWranglerTransformationStepsManagerImpl<C : DataWranglerContext> : DataWranglerTransformationStepsManager<C> {

  private val transformationsFlow = MutableSharedFlow<List<TransformationStep<*, C>>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
  //todo: provide states
  private val errorsFlow = MutableSharedFlow<Throwable>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

  private val history: MutableList<TransformationStep<*, C>> = synchronizedList(LinkedList())

  override fun getErrorsFlow(): Flow<Throwable> = errorsFlow

  override fun removeTransformation(step: TransformationStep<*, *>) {
    history.remove(step)
    transformationsFlow.tryEmit(history)
  }

  override fun addTransformation(step: TransformationStep<*, C>) {
    DataWranglerProviderCollector.logDWOperationExecuted(step.createCommand())
    history.add(step)
    transformationsFlow.tryEmit(history)
  }

  override fun getTransformationsList(): List<TransformationStep<*, C>> = history.toList()

  override fun resetTransformationsTo(step: TransformationStep<*, *>) {
    val index = findIndexByStep(step) ?: return
    history.subList(index + 1, history.size).clear()
    transformationsFlow.tryEmit(history)
  }

  override fun findIndexByStep(step: TransformationStep<*, *>): Int? = history.indexOf(step).takeIf { it != -1 }

  override fun getEventFlow(): Flow<List<TransformationStep<*, out DataWranglerContext>>> = transformationsFlow
  override fun getExecutedCommands(): List<DataWranglerCommand<C>> = getTransformationsList().map { it.createCommand() }
  override fun reportState(step: TransformationStep<*, C>, state: StepState) {
    when (state) {
      is StepState.Failed ->
        errorsFlow.tryEmit(state.th)
      else -> Unit
    }
  }

  companion object {

    fun <C : DataWranglerContext> createDefault(context: C): DataWranglerTransformationStepsManagerImpl<C> {
      return DataWranglerTransformationStepsManagerImpl()
    }
  }
}