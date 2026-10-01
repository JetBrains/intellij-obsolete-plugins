package com.intellij.dataWrangler.operations

import com.intellij.dataWrangler.executor.DataWranglerContext
import org.jetbrains.annotations.Nls

interface DataWranglerCommand<C : DataWranglerContext> {

  /**
   * User-visible command name in the transformation history after execution
   */
  fun getCommandLabel(): @Nls String

  /**
   * User-visible description in the transformation history
   */
  fun getDescription(): @Nls String


  /**
   *  Function to execute command in the current context
   */
  suspend fun execute(context: C)
}