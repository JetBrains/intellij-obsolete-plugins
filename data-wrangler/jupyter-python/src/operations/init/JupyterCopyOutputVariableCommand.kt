package com.intellij.dataWrangler.jupyterPython.operations.init

import com.intellij.dataWrangler.impl.operations.CommandFactoryBase
import com.intellij.dataWrangler.jupyterPython.DataWranglerJupyterPyBundle
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.dataWrangler.operations.TransformationStep
import com.intellij.dataWrangler.operations.DataWranglerCommand
import org.jetbrains.annotations.Nls

class JupyterCopyOutputVariableParams

class JupyterCopyOutputVariableFactory : CommandFactoryBase<JupyterCopyOutputVariableParams, PythonDataWranglerContext>(JupyterCopyOutputVariableParams::class,
                                                                                                                        DataWranglerJupyterPyBundle.messagePointer("data.wrangler.jupyter.command.init.name")) {
  override fun createCommand(parameters: JupyterCopyOutputVariableParams): DataWranglerCommand<PythonDataWranglerContext> = JupyterCopyOutputVariableCommand()
}

class JupyterCopyOutputVariableCommand : DataWranglerCommand<PythonDataWranglerContext> {

  override fun getCommandLabel(): @Nls String = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.init.label")
  override fun getDescription(): @Nls String = DataWranglerJupyterPyBundle.message("data.wrangler.jupyter.command.init.description")

  override suspend fun execute(context: PythonDataWranglerContext) {
    context.executeCommand("import pandas as pd\n${context.getTableName()} = ${context.getVariableCodePreviewName()}.copy(deep=True)")
  }

  companion object {
    fun createStep(): TransformationStep<*, PythonDataWranglerContext> {
      return TransformationStep(JupyterCopyOutputVariableFactory(), JupyterCopyOutputVariableParams())
    }
  }
}