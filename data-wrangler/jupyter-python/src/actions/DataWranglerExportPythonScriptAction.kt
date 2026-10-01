package com.intellij.dataWrangler.jupyterPython.actions

import com.intellij.dataWrangler.DataWranglerSession
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.writeText
import com.jetbrains.python.PythonLanguage

internal class DataWranglerExportPythonScriptAction : DataWranglerExportCodeAction() {
  override fun getTargetLanguage(): Language = PythonLanguage.INSTANCE

  override suspend fun <C : PythonDataWranglerContext> getFileContent(session: DataWranglerSession<C>): String? {
    val context = session.getContext()
    val previewProvider = session.getCodePreviewProvider() ?: return null
    val commands = session.getTransformationStepsManager().getExecutedCommands()
    return previewProvider.getTransformationCode(context, commands)
  }

  override fun writeFile(
    outputFile: VirtualFile,
    project: Project,
    content: String,
  ) {
    outputFile.writeText(content)
  }
}