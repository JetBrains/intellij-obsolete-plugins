package com.intellij.aidebugger.python.utility

import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.QualifiedName
import com.jetbrains.python.PythonFileType
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyFromImportStatement
import com.jetbrains.python.psi.PyImportStatement


suspend fun hasImportsOfInterest(
  project: Project,
  file: VirtualFile,
  packages: Set<String>
): Boolean = readAction {
  if (file.fileType !is PythonFileType) return@readAction false

  val psiManager = PsiManager.getInstance(project)
  val pyFile = psiManager.findFile(file) as? PyFile ?: return@readAction false

  return@readAction pyFile
    .getImports()
    .any { qn -> packages.any { qn.firstComponent?.startsWith(it) == true } }
}

suspend fun hasImportsOfInterest(
  project: Project,
  files: Sequence<VirtualFile>,
  packages: Set<String>
): Boolean = readAction {
  val psiManager = PsiManager.getInstance(project)

  return@readAction files
    .filter { it.fileType is PythonFileType }
    .map { psiManager.findFile(it) }
    .filterIsInstance<PyFile>()
    .flatMap { it.getImports() }
    .any { qn -> packages.any { qn.firstComponent?.startsWith(it) == true } }
}

@Suppress("UnstableApiUsage")
fun PyFile.getImports(): Sequence<QualifiedName> = sequence {
  val pyFile = this@getImports

  val importBlock = pyFile.importBlock ?: return@sequence

  importBlock.forEach { import ->
    when (import) {
      is PyImportStatement -> import.importElements.forEach { elem ->
        elem.importedQName?.let { yield(it) }
      }
      is PyFromImportStatement -> import.importSourceQName?.let { yield(it) }
    }
  }
}