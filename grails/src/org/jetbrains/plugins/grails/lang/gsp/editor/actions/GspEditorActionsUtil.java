// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.editor.actions;

import com.intellij.application.options.CodeStyle;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.plugins.grails.fileType.GspFileType;
import org.jetbrains.plugins.grails.lang.gsp.lexer.core.GspTokenTypes;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyElementType;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyTokenTypes;

public final class GspEditorActionsUtil {

  private GspEditorActionsUtil() {

  }

  public static void insertSpacesByGspIndent(Editor editor, Project project) {
    int indentSize = CodeStyle.getSettings(editor).getIndentSize(GspFileType.GSP_FILE_TYPE);
    EditorModificationUtil.insertStringAtCaret(editor, StringUtil.repeatSymbol(' ', indentSize));
  }

  public static boolean areSciptletSeparatorsUnbalanced(HighlighterIterator iterator) {
    IElementType firstElementType = iterator.getTokenType();
    assert firstElementType == GspTokenTypes.JSCRIPT_BEGIN || firstElementType == GspTokenTypes.GSCRIPT_BEGIN;
    iterator.advance();

    IElementType prev = null;

    while (!iterator.atEnd()) {
      IElementType element = iterator.getTokenType();

      if (element instanceof GroovyElementType) {
        if (element == GroovyTokenTypes.mMOD && prev == GroovyTokenTypes.mLT
            || element == GroovyTokenTypes.mLCURLY && prev == GroovyTokenTypes.mMOD
            || element == GroovyTokenTypes.mMOD_ASSIGN && prev == GroovyTokenTypes.mLT) {
          return true;
        }
      }
      else if (element == GspTokenTypes.JSCRIPT_END || element == GspTokenTypes.GSCRIPT_END || element == GspTokenTypes.JEXPR_BEGIN) {
        return false;
      }

      prev = element;

      iterator.advance();
    }

    return true;
  }

}
