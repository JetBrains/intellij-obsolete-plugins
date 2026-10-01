package com.intellij.dataWrangler.jupyterPython.operations.custom

import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.dataWrangler.jupyterPython.operations.JupyterPyCommandBase
import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.dataWrangler.operations.FieldType
import com.intellij.dataWrangler.operations.MetaStruct
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.annotations.Nls

internal class JupyterCustomCommand(factory: CommandFactory<JupyterCustomCommandParameters, PythonDataWranglerContext>, val info: JupyterCustomCommandInfo, parameters: JupyterCustomCommandParameters)
  : JupyterPyCommandBase<JupyterCustomCommandParameters>(factory, parameters) {
  override fun getCommandLabel(): @Nls String =
    info.func.label?.interpolate(factory.parametersMetaType, parameters) ?: factory.commandName

  override fun getDescription(): @Nls String =
    info.func.details?.interpolate(factory.parametersMetaType, parameters) ?: ""

  override suspend fun generate(context: TableInfoData, generator: CodeGenerator) {
    generator.addImport(VfsUtil.loadText(info.file) + "\n")
    val v = context.getTableName()
    val arguments = factory.parametersMetaType.getFields().values.joinToString("") { ", ${renderArgument(it)}" }
    generator.addCommand("$v = ${info.func.id}($v$arguments)")
  }

  private fun renderArgument(type: FieldType<JupyterCustomCommandParameters, *>): String =
    type.get(parameters).renderValue()

}

private fun @Nls String.interpolate(meta: MetaStruct<JupyterCustomCommandParameters>, parameters: JupyterCustomCommandParameters): @Nls String {
  @Suppress("HardCodedStringLiteral")
  return Regex("\\{([^}]*)}").replace(this) {
    val name = it.groups[1]?.value
    val value = meta.getFields().values.find { it.id == name }?.get(parameters)
    value?.renderValueForInterpolation() ?: ""
  }
}

private fun Any?.renderValue(): String =
  when (this) {
    is String -> pyString
    is Collection<*> -> "[${joinToString { it.renderValue() }}]"
    null -> "None"
    else -> toString()
  }

private fun Any?.renderValueForInterpolation(): @NlsSafe String =
  when (this) {
    is String -> this
    is Collection<*> -> if (isEmpty()) "[]" else joinToString { it.renderValueForInterpolation() }
    else -> renderValue()
  }

private val String.pyString get() = "'${replace("'", "\\'")}'"

