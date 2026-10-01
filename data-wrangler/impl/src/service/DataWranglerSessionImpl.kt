package com.intellij.dataWrangler.impl.service

import com.intellij.dataWrangler.DataWranglerSession
import com.intellij.dataWrangler.executor.CodePreviewProvider
import com.intellij.dataWrangler.executor.DataWranglerContext
import com.intellij.dataWrangler.executor.DataWranglerEngine
import com.intellij.dataWrangler.executor.DataWranglerTransformationStepsManager
import com.intellij.dataWrangler.executor.DataWranglerTransformationStepsManager.StepState
import com.intellij.dataWrangler.operations.TransformationStep
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.platform.util.coroutines.childScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val LOG = fileLogger()

class DataWranglerSessionImpl<C : DataWranglerContext>(
  parentCoroutineScope: CoroutineScope,
  private val engine: DataWranglerEngine<C>,
  private val transformationManager: DataWranglerTransformationStepsManager<C>,
  private val previewProvider: CodePreviewProvider<C>?,
  private val context: C,
) : DataWranglerSession<C> {
  private val coroutineScope = parentCoroutineScope.childScope("DW Session $context", supervisor = true)

  init {
    engine.getInitialStep(context)?.let { transformationManager.addTransformation(it) }
  }

  override fun dispose() {
    context.dispose()
    coroutineScope.cancel()
  }

  override suspend fun runTransformations(steps: List<TransformationStep<*, C>>) {
    withContext(Dispatchers.Default) {
      steps.forEach { runTransformationImpl(it) }
    }
  }

  override fun runTransformation(step: TransformationStep<*, C>) {
    coroutineScope.launch(Dispatchers.Default) {
      runTransformationImpl(step)
    }
  }

  private suspend fun runTransformationImpl(step: TransformationStep<*, C>) {
    try {
      executeWithReporting(step)
      transformationManager.addTransformation(step)
    }
    catch (e: CancellationException) {
      throw e
    }
    catch (ignored: Throwable) {
    }
  }

  private suspend fun executeWithReporting(step: TransformationStep<*, C>) {
    try {
      reportState(step, StepState.Running)
      step.createCommand().execute(context)
      reportState(step, StepState.Ok)
    }
    catch (e: Throwable) {
      reportState(step, if (e is CancellationException) StepState.Cancelled else StepState.Failed(e))
      throw e
    }
  }

  private fun reportState(step: TransformationStep<*, C>, s: StepState) {
    transformationManager.reportState(step, s)
  }

  override fun rerunSession() {
    coroutineScope.launch(Dispatchers.Default) {
      val steps = transformationManager.getTransformationsList()
      val iterator = steps.iterator()
      try {
        iterator.forEach { step ->
          executeWithReporting(step)
        }
      }
      catch (_: Throwable) {
        iterator.forEach { step ->
          reportState(step, StepState.Cancelled)
        }
      }
    }
  }

  override fun getContext(): C = context
  override fun getCodePreviewProvider(): CodePreviewProvider<C>? = previewProvider
  override fun getEngine(): DataWranglerEngine<C> = engine
  override fun getTransformationStepsManager(): DataWranglerTransformationStepsManager<C> = transformationManager
}