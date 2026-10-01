package com.intellij.dataWrangler.operations

import com.intellij.dataWrangler.executor.DataWranglerContext

data class TransformationStep<P: Any, C: DataWranglerContext>(val factory: CommandFactory<P, C>, val params: P) {
  fun createCommand(): DataWranglerCommand<C> = factory.createCommand(params)
}