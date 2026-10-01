package com.intellij.dataWrangler.jupyterPython.operations.init

import com.intellij.dataWrangler.impl.operations.CommandFactoryBase
import com.intellij.dataWrangler.jupyterPython.DataWranglerJupyterPyBundle
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.dataWrangler.jupyterPython.engine.getInitDataFrameCode
import com.intellij.dataWrangler.operations.DataWranglerCommand
import com.intellij.dataWrangler.operations.TransformationStep
import org.jetbrains.annotations.Nls

class PythonCreateDataFrameParams

class PythonCreateDataFrameFactory : CommandFactoryBase<PythonCreateDataFrameParams, PythonDataWranglerContext>(PythonCreateDataFrameParams::class,
                                                                                                                DataWranglerJupyterPyBundle.messagePointer("data.wrangler.py.local.file.init.name")) {
  override fun createCommand(parameters: PythonCreateDataFrameParams): DataWranglerCommand<PythonDataWranglerContext> = PythonCreateDataFrameCommand()
}

class PythonCreateDataFrameCommand : DataWranglerCommand<PythonDataWranglerContext> {

  override fun getCommandLabel(): @Nls String = DataWranglerJupyterPyBundle.message("data.wrangler.py.local.file.init.label")
  override fun getDescription(): @Nls String = DataWranglerJupyterPyBundle.message("data.wrangler.py.local.file.init.description")

  override suspend fun execute(context: PythonDataWranglerContext) {
    context.executeCommand("import pandas as pd\n${getCommandCode(context.getTableName(), context)}")
  }

  private fun getCommandCode(pythonVariableName: String, context: PythonDataWranglerContext): String {
    val file = context.getFile() ?: return ""
    return getInitDataFrameCode(context.getProject(), pythonVariableName, file)
  }

  companion object {
    fun createStep(): TransformationStep<*, PythonDataWranglerContext> {
      return TransformationStep(PythonCreateDataFrameFactory(), PythonCreateDataFrameParams())
    }
  }
}