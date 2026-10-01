package com.intellij.dataWrangler.jupyterPython.operations.custom

import com.intellij.dataWrangler.jupyterPython.operations.custom.JupyterCustomCommandInfo.ArgInfo
import com.intellij.dataWrangler.jupyterPython.operations.custom.JupyterCustomCommandInfo.FunctionInfo
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.util.text.CharSequenceSubSequence
import com.jetbrains.python.ProtectionLevel
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyParameter
import kotlinx.coroutines.CancellationException
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls

private val logger = fileLogger()

object JupyterCustomCommandParser {
  internal fun parseCustomCommands(project: Project, file: VirtualFile): List<JupyterCustomCommandInfo> =
    ReadAction.compute<List<JupyterCustomCommandInfo>, RuntimeException> {
      parse(project, file).map {
        JupyterCustomCommandInfo(file, it.info)
      }
    }

  fun findCustomCommands(project: Project, file: VirtualFile, id: String): FunctionParsedInfo? =
    ReadAction.compute<FunctionParsedInfo?, RuntimeException> {
      parse(project, file).find { it.info.id == id }
    }

  data class FunctionParsedInfo(val info: FunctionInfo, val prefixRange: TextRange)

  private data class ParsedDocString(
    var displayName: @Nls String? = null,
    var description: @Nls String? = null,
    var label: @Nls String? = null,
    var details: @Nls String? = null,
    var group: @NonNls String? = null,
    val args: MutableMap<String, ArgInfo> = mutableMapOf()
  )

  private fun parse(project: Project, defFile: VirtualFile): List<FunctionParsedInfo> {
    return try {
      val file = PsiManager.getInstance(project).findFile(defFile) ?: return emptyList()
      getOperatorFunctions(file).mapNotNull { f ->
        return@mapNotNull parseFunction(f)
      }
    }
    catch (c: CancellationException) {
      throw c
    }
    catch (th: Throwable) {
      logger.error(th)
      emptyList()
    }
  }

  fun getOperatorFunctions(file: PsiFile): List<PyFunction> {
    return (file as? PyFile)?.topLevelFunctions?.filter { isOperationFunction(it) } ?: emptyList()
  }

  fun isOperationFunction(f: PyFunction): Boolean =
    f.protectionLevel == ProtectionLevel.PUBLIC && f.name != null

  private fun parseFunction(f: PyFunction): FunctionParsedInfo? {
    if (!isOperationFunction(f)) return null
    val id = f.name ?: return null
    return try {
      val fromDocString = parseDocString(f.docStringValue)
      FunctionParsedInfo(
        FunctionInfo(id, fromDocString.displayName ?: id, fromDocString.description ?: "", fromDocString.group ?: "", fromDocString.label, fromDocString.details, f.parameterList.parameters.map {
          parseArg(it, fromDocString)
        }),
        TextRange.create(f.textRange.startOffset, (f.docStringExpression ?: f).textRange.endOffset)
      )
    }
    catch (c: CancellationException) {
      throw c
    }
    catch (th: Throwable) {
      logger.warn(th)
      null
    }
  }

  private fun parseArg(parameter: PyParameter, fromDocString: ParsedDocString): ArgInfo {
    val argName = parameter.name ?: ""
    val def = parameter.defaultValueText
    return fromDocString.args[argName]?.copy(def = def) ?: ArgInfo(argName, null, null, def)
  }

  internal const val DISPLAY_NAME = "display-name"
  internal const val DESCRIPTION = "description"
  internal const val LABEL = "label"
  internal const val DETAILS = "details"
  internal const val PARAM = "param"
  internal const val TYPE = "type"
  internal const val GROUP = "group"

  private fun parseDocString(docStr: String?): ParsedDocString = ParsedDocString().apply {
    if (docStr == null) return@apply
    Regex(":($DISPLAY_NAME|$DESCRIPTION|$LABEL|$DETAILS|$PARAM|$GROUP)\\s+(.*)").findAll(docStr).forEach { m ->
      @Suppress("HardCodedStringLiteral")
      when (m.groups[1]?.value) {
        DISPLAY_NAME -> displayName = m.groups[2]?.value?.trim()
        DESCRIPTION -> description = m.groups[2]?.value?.trim()
        LABEL -> label = m.groups[2]?.value?.trim()
        DETAILS -> details = m.groups[2]?.value?.trim()
        GROUP -> group = m.groups[2]?.value?.trim()
        PARAM -> {
          m.groups[2]?.let { g ->
            parseParam(docStr, g)
          }
        }
      }
    }
  }

  private fun ParsedDocString.parseParam(docStr: String, g: MatchGroup) {
    docStr.indexOf(':', g.range.first).takeIf { it in g.range }?.let { cIdx ->
      val params = CharSequenceSubSequence(docStr, cIdx, g.range.last + 1)
      var displayName: String? = null
      var type: String? = null
      Regex(":($DISPLAY_NAME|$TYPE)\\s+([^:]*)").findAll(params).forEach { sm ->
        when (sm.groups[1]?.value) {
          DISPLAY_NAME -> displayName = sm.groups[2]?.value?.trim() //NON-NLS
          TYPE -> type = sm.groups[2]?.value?.trim()
        }
      }
      val id = docStr.substring(g.range.start, cIdx)
      args[id] = ArgInfo(id, displayName, type, null)
    }
  }
}