package com.intellij.dataWrangler.jupyterPython.operations

import com.intellij.dataWrangler.impl.operations.DWTypes
import com.intellij.dataWrangler.impl.operations.toMetaStructure
import com.intellij.dataWrangler.jupyterPython.DataWranglerJupyterPyBundle
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.dataWrangler.jupyterPython.operations.custom.JupyterCustomCommandParameters
import com.intellij.dataWrangler.jupyterPython.operations.custom.buildFieldForValue
import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.dataWrangler.operations.MetaStruct
import com.intellij.dataWrangler.operations.TransformationStep
import org.jetbrains.annotations.Nls

internal fun createMissingJupyterTransformationStep(factoryId: String, params: Map<String, Any>): TransformationStep<JupyterCustomCommandParameters, PythonDataWranglerContext> {
  val cp = JupyterCustomCommandParameters(params.toMutableMap())
  val mt = toMetaStructure(
    factoryId, JupyterCustomCommandParameters::class,
    DWTypes.createFakeStruct(
      JupyterCustomCommandParameters::class,
      params.map { buildFieldForValue(it.key, it.value) },
    )
  )
  return TransformationStep(JupyterMissingFactory(factoryId, mt), cp)
}

private class JupyterMissingFactory(override val id: String, override val parametersMetaType: MetaStruct<JupyterCustomCommandParameters>): CommandFactory<JupyterCustomCommandParameters, PythonDataWranglerContext> {
  override val commandName: @Nls String
    get() = DataWranglerJupyterPyBundle.message("unknown.command.0", id)

  override fun createCommand(parameters: JupyterCustomCommandParameters): JupyterMissingCommand = JupyterMissingCommand(this, parameters)
}

private class JupyterMissingCommand(factory: CommandFactory<JupyterCustomCommandParameters, PythonDataWranglerContext>, parameters: JupyterCustomCommandParameters) : JupyterPyCommandBase<JupyterCustomCommandParameters>(factory, parameters) {
  override fun getCommandLabel(): @Nls String = factory.commandName

  override fun getDescription(): @Nls String = factory.commandName
  override suspend fun generate(context: TableInfoData, generator: CodeGenerator) {
    generator.addCommand("# ${getCommandLabel()}")
  }
}

