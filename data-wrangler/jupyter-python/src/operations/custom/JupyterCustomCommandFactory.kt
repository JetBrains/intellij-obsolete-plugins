package com.intellij.dataWrangler.jupyterPython.operations.custom

import com.intellij.dataWrangler.impl.operations.DWTypes
import com.intellij.dataWrangler.impl.operations.toMetaStructure
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.dataWrangler.operations.CommandFactoryGroup
import com.intellij.dataWrangler.operations.DataWranglerCommand
import com.intellij.dataWrangler.operations.MetaStruct
import com.intellij.util.text.nullize
import org.jetbrains.annotations.Nls

data class JupyterCustomCommandParameters(val props: MutableMap<String, Any> = mutableMapOf())

class JupyterCustomCommandFactory(val info: JupyterCustomCommandInfo) : CommandFactory<JupyterCustomCommandParameters, PythonDataWranglerContext> {
  override val id: String
    get() = "custom.${info.file.nameWithoutExtension}.${info.func.id}"
  override val commandName: @Nls String
    get() = info.func.run { displayName ?: id }

  override fun getDescription(): String? =
    info.func.description.nullize()

  override fun getGroupName(): CommandFactoryGroup {
    return CommandFactoryGroup.entries.find { it.name.equals(info.func.group, true) } ?: CommandFactoryGroup.CUSTOM
  }

  override val parametersMetaType: MetaStruct<JupyterCustomCommandParameters> = toMetaStructure(
    info.func.id, JupyterCustomCommandParameters::class,
    DWTypes.createFakeStruct(
      JupyterCustomCommandParameters::class,
      info.func.args.asSequence().drop(1).map { it.asField() }.toList()
    )
  )
  override fun createCommand(parameters: JupyterCustomCommandParameters): DataWranglerCommand<PythonDataWranglerContext> =
    JupyterCustomCommand(this, info, parameters)

  override fun equals(other: Any?): Boolean {
    return other is JupyterCustomCommandFactory && info == other.info
  }

  override fun hashCode(): Int {
    return info.hashCode()
  }
}

