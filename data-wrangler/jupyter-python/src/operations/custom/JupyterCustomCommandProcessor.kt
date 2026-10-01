package com.intellij.dataWrangler.jupyterPython.operations.custom

import com.intellij.dataWrangler.jupyterPython.DataWranglerJupyterPyBundle
import com.intellij.dataWrangler.jupyterPython.operations.custom.JupyterCustomCommandInfo.ArgInfo
import com.intellij.dataWrangler.jupyterPython.operations.custom.JupyterCustomCommandInfo.FunctionInfo
import com.intellij.dataWrangler.jupyterPython.operations.custom.JupyterCustomCommandParser.DESCRIPTION
import com.intellij.dataWrangler.jupyterPython.operations.custom.JupyterCustomCommandParser.DETAILS
import com.intellij.dataWrangler.jupyterPython.operations.custom.JupyterCustomCommandParser.DISPLAY_NAME
import com.intellij.dataWrangler.jupyterPython.operations.custom.JupyterCustomCommandParser.GROUP
import com.intellij.dataWrangler.jupyterPython.operations.custom.JupyterCustomCommandParser.LABEL
import com.intellij.dataWrangler.jupyterPython.operations.custom.JupyterCustomCommandParser.TYPE
import com.intellij.dataWrangler.jupyterPython.operations.pyStr
import com.intellij.dataWrangler.operations.CommandFactoryGroup
import com.intellij.database.util.common.isNotNullOrEmpty
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.createParentDirectories
import kotlin.io.path.createFile
import kotlin.io.path.exists

object JupyterCustomCommandProcessor {
  fun updateCustomCommand(
    project: Project,
    file: VirtualFile,
    found: JupyterCustomCommandParser.FunctionParsedInfo,
    newInfo: FunctionInfo,
  ): TextRange {
    val prefix = generateFunctionPrefix(newInfo)
    WriteCommandAction.writeCommandAction(project)
      .withName(DataWranglerJupyterPyBundle.message("command.name.update.command"))
      .run<RuntimeException> {
        val document = FileDocumentManager.getInstance().getDocument(file)!!
        document.replaceString(found.prefixRange.startOffset, found.prefixRange.endOffset, prefix)
      }
    return TextRange.from(found.prefixRange.startOffset, prefix.length)
  }

  fun generateCustomCommand(
    project: Project,
    fileName: String,
    newInfo: FunctionInfo,
  ): Pair<TextRange, JupyterCustomCommandInfo>? {
    return WriteCommandAction.writeCommandAction(project)
      .withName(DataWranglerJupyterPyBundle.message("command.name.update.command"))
      .compute<Pair<TextRange, JupyterCustomCommandInfo>?, RuntimeException> {
        val path = JupyterCustomCommandFactories.getRootPath().resolve(fileName)
        path.createParentDirectories()
        if (!path.exists()) path.createFile()
        val vFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path) ?: return@compute null
        val document = FileDocumentManager.getInstance().getDocument(vFile) ?: return@compute null
        val originalLength = document.textLength
        val toAppend = buildString {
          if (originalLength != 0) {
            append("\n\n")
          }
          generateFunctionPrefix(newInfo)
          append("    return #TODO: new data frame\n")
        }
        document.replaceString(originalLength, originalLength, toAppend)
        Pair(
          TextRange.from(originalLength, toAppend.length),
          JupyterCustomCommandInfo(vFile, newInfo)
        )
      }
  }


  private fun generateFunctionPrefix(f: FunctionInfo): String = buildString {
    generateFunctionPrefix(f)
  }
  private fun StringBuilder.generateFunctionPrefix(f: FunctionInfo) {
    generateSignature(f)
    append("    \"\"\"\n")
    generateDocString(f, "    ")
    append("    \"\"\"\n")
  }
  private fun StringBuilder.generateSignature(f: FunctionInfo) {
    append("def ").append(f.id).append("(")
    f.args.forEachIndexed { index, a ->
      if (index != 0) append(", ")
      generateArgument(a)
    }
    append("):\n")
  }
  private fun StringBuilder.generateArgument(a: ArgInfo) {
    append(a.id)
    if (a.def.isNotNullOrEmpty) {
      append(" = ")
      append(if (a.type.shouldQuote()) a.def.pyStr else a.def)
    }
  }

  private fun String?.shouldQuote(): Boolean =
    when {
      this === null -> true
      startsWith("int", true) ||
      startsWith("float", true) ||
      startsWith("list", true) ||
      startsWith("set", true) -> false
      else -> true
    }

  private fun StringBuilder.generateDocString(f: FunctionInfo, ident: String) {
    if (f.displayName.isNotNullOrEmpty) {
      append(ident).append(":").append(DISPLAY_NAME).append(" ").append(f.displayName).append("\n")
    }
    if (f.description.isNotNullOrEmpty) {
      append(ident).append(":").append(DESCRIPTION).append(" ").append(f.description).append("\n")
    }
    if (f.group.isNotNullOrEmpty && f.group != CommandFactoryGroup.CUSTOM.name) {
      append(ident).append(":").append(GROUP).append(" ").append(f.group).append("\n")
    }
    if (f.label.isNotNullOrEmpty) {
      append(ident).append(":").append(LABEL).append(" ").append(f.label).append("\n")
    }
    if (f.details.isNotNullOrEmpty) {
      append(ident).append(":").append(DETAILS).append(" ").append(f.details).append("\n")
    }
    f.args.forEach {
      generateDocString(it, ident)
    }
  }

  private fun StringBuilder.generateDocString(a: ArgInfo, ident: String) {
    append(ident)
    append(":param ").append(a.id).append(":")
    if (a.displayName.isNotNullOrEmpty) {
      append(" :").append(DISPLAY_NAME).append(" ").append(a.displayName)
    }
    if (a.type.isNotNullOrEmpty) {
      append(" :").append(TYPE).append(" ").append(a.type)
    }
    append("\n")
  }
}