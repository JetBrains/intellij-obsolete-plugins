package com.intellij.dataWrangler.executor

import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.dataWrangler.operations.TransformationStep
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.extensions.ExtensionPointName
import kotlinx.coroutines.flow.StateFlow

interface DataWranglerEngine<C : DataWranglerContext> {
  val id: String
  /**
   * A function to create a context for a given engine.
   * isExecutionAvailable should always be called before creating context
   */
  fun createInitialContext(dataContext: DataContext): C?

  /**
   * Return all transformations over table, that available in the current engine.
   */
  fun commandsFactories(): StateFlow<List<CommandFactory<*, C>>>

  /**
   * Return code preview manager
   */
  fun getPreviewProvider(): CodePreviewProvider<C>? = null

  /**
   * Checks all requirements needed to create initial context, but it doesn't create context
   */
  fun canCreateDWContext(dataContext: DataContext): Boolean

  /**
   * Provided a command is needed for reverting table to original state
   * This command will be included in the transformation list without executing
   */
  fun getInitialStep(context: C): TransformationStep<*, C>? = null

  companion object {
    val EP: ExtensionPointName<DataWranglerEngine<*>> = ExtensionPointName.create("com.intellij.dataWrangler.engine")
  }
}