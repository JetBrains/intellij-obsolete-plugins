package com.intellij.dataWrangler.jupyterPython.engine

import com.intellij.dataWrangler.executor.CodePreviewProvider
import com.intellij.dataWrangler.jupyterPython.operations.JupyterPyCommandBase
import com.intellij.dataWrangler.jupyterPython.operations.asContextData
import com.intellij.dataWrangler.operations.DataWranglerCommand
import com.jetbrains.python.PythonFileType

private class CodeExtractorDataContext(val delegate: JupyterPyCommandBase.TableInfoData, val varName: String) : JupyterPyCommandBase.TableInfoData by delegate {
  constructor(context: PythonDataWranglerContext, varName: String) : this(context.asContextData(), varName)

  override fun getTableName(): String = varName
}

internal class CodeExtractingGenerator : JupyterPyCommandBase.CodeGenerator {

  val imports = StringBuilder()
  val code = StringBuilder()

  override fun addImport(importCode: String) {
    imports.append(importCode)
    imports.append("\n")
  }

  override fun addCommand(commandCode: String) {
    code.append(commandCode)
    code.append("\n")
  }

  override fun setCommandWrapper(codeWrapper: String) {
    addCommand(codeWrapper)
  }

  fun getFullCode(): String {
    val res = StringBuilder(imports)
    res.append(code)
    return res.toString()
  }
}

class PyCodePreviewProvider : CodePreviewProvider<PythonDataWranglerContext> {

  override suspend fun getCodePreview(context: PythonDataWranglerContext, command: DataWranglerCommand<PythonDataWranglerContext>): String {
    if (command !is JupyterPyCommandBase<*>) return ""
    val extractor = CodeExtractingGenerator()
    command.generate(CodeExtractorDataContext(context, context.getVariableCodePreviewName()), extractor)
    return extractor.code.toString()
  }

  override fun isCommandApplicable(command: DataWranglerCommand<PythonDataWranglerContext>): Boolean {
    return command is JupyterPyCommandBase<*>
  }

  override fun getFileType(): PythonFileType = PythonFileType.INSTANCE

  override suspend fun getTransformationCode(
    context: PythonDataWranglerContext,
    commands: List<DataWranglerCommand<PythonDataWranglerContext>>,
  ): String {
    val initCode: String = context.getInitializationCode()
    val context = CodeExtractorDataContext(context, DW_VARIABLE_NAME)
    return getTransformationCode(context, initCode, commands)
  }

  suspend fun getTransformationCode(context: JupyterPyCommandBase.TableInfoData, initCode: String, commands: List<DataWranglerCommand<*>>): String {
    val extractor = CodeExtractingGenerator()

    extractor.addCommand(initCode)
    commands.forEach { command ->
      if (command is JupyterPyCommandBase<*>) {
        extractor.addCommand("# ${command.getCommandLabel()}")
        command.generate(context, extractor)
      }
    }

    extractor.addCommand(context.getTableName())
    return extractor.getFullCode()
  }
}