/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.lang.gsp.highlighter

import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.highlighter.EditorHighlighter
import com.intellij.openapi.fileTypes.EditorHighlighterProvider
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

internal class GspEditorHighlighterProvider : EditorHighlighterProvider {
  override fun getEditorHighlighter(project: Project?,
                                    fileType: FileType,
                                    virtualFile: VirtualFile?,
                                    colors: EditorColorsScheme): EditorHighlighter {
    return GspEditorHighlighter(colors, project, virtualFile)
  }
}
