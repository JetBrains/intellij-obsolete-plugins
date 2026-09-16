// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bigdatatools.zeppelin.notebook.interpreter

import com.intellij.bigdatatools.notebooks.core.impl.editor.colors.CELL_MARKER
import com.intellij.bigdatatools.notebooks.zeppelin.psi.ZeppelinTypes
import com.intellij.bigdatatools.zeppelin.notebook.lexer.ZeppelinFileBasedTemplateLexer
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.ex.util.LayerDescriptor
import com.intellij.openapi.editor.ex.util.LayeredLexerEditorHighlighter
import com.intellij.openapi.editor.highlighter.EditorHighlighter
import com.intellij.openapi.fileTypes.EditorHighlighterProvider
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType

class ZeppelinSyntaxHighlighter(val project: Project?, val virtualFile: VirtualFile?) : SyntaxHighlighterBase() {
  override fun getTokenHighlights(tokenType: IElementType): Array<out TextAttributesKey> =
    if (ZeppelinTypes.CODE_MARKER == tokenType)
      arrayOf(CELL_MARKER)
    else
      TextAttributesKey.EMPTY_ARRAY

  override fun getHighlightingLexer() = ZeppelinFileBasedTemplateLexer(project, virtualFile)
}

class ZeppelinEditorHighlighterProvider : EditorHighlighterProvider {
  override fun getEditorHighlighter(project: Project?,
                                    fileType: FileType,
                                    virtualFile: VirtualFile?,
                                    colors: EditorColorsScheme): EditorHighlighter =
    ZeppelinTemplateEditorHighlighter(ZeppelinSyntaxHighlighter(project, virtualFile), project, virtualFile, colors)
}

class ZeppelinTemplateEditorHighlighter(syntaxHighlighter: SyntaxHighlighter,
                                        project: Project?,
                                        virtualFile: VirtualFile?,
                                        colors: EditorColorsScheme) : LayeredLexerEditorHighlighter(syntaxHighlighter, colors) {
  init {
    ZeppelinInterpreters.getSourcesToHighlight(project, virtualFile).forEach { (source, highlighter) ->
      val layerDescriptor = LayerDescriptor(highlighter, "", null)
      registerLayer(source, layerDescriptor)
    }
  }
}