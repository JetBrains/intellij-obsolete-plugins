package com.intellij.dbt.typing

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.jinja.Jinja2FileType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class DbtJinjaTypedHandler : TypedHandlerDelegate() {
  override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
    if (file.fileType != Jinja2FileType.INSTANCE) return Result.CONTINUE
    if (c != '"' && c != '\'') return Result.CONTINUE
    val caretOffset = editor.caretModel.offset
    if (caretOffset > 1 && editor.document.text[caretOffset - 2] != c) {
      editor.document.insertString(caretOffset, c.toString())
    }
    return Result.CONTINUE
  }
}