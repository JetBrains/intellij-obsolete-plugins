package com.intellij.dataWrangler

import com.intellij.dataWrangler.executor.CodePreviewProvider
import com.intellij.dataWrangler.executor.DataWranglerContext
import com.intellij.dataWrangler.executor.DataWranglerEngine
import com.intellij.dataWrangler.executor.DataWranglerTransformationStepsManager
import com.intellij.dataWrangler.operations.TransformationStep
import com.intellij.openapi.actionSystem.DataKey

interface DataWranglerSession<C : DataWranglerContext> {

  suspend fun runTransformations(steps: List<TransformationStep<*, C>>)

  fun runTransformation(step: TransformationStep<*, C>)

  fun rerunSession()

  fun getContext(): C

  fun getCodePreviewProvider(): CodePreviewProvider<C>?

  fun getEngine(): DataWranglerEngine<C>

  fun getTransformationStepsManager(): DataWranglerTransformationStepsManager<C>

  fun dispose()
}

@Suppress("UNCHECKED_CAST")
inline fun <reified C: DataWranglerContext> DataWranglerSession<*>.asSafely(): DataWranglerSession<C>? =
  if (getContext() is C) this as DataWranglerSession<C> else null

val DW_SESSION: DataKey<DataWranglerSession<*>> = DataKey.create<DataWranglerSession<*>>("DW_SESSION")
