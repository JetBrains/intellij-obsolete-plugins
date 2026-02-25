// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.editor.actions;

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.xml.XmlTokenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.addins.js.JavaScriptIntegrationUtil;
import org.jetbrains.plugins.grails.lang.gsp.lexer.core.GspTokenTypes;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspFile;
import org.jetbrains.plugins.groovy.editor.HandlerUtils;

import static org.jetbrains.plugins.grails.lang.gsp.lexer.core.GspTokenTypes.GEXPR_END;

public final class GspTypedHandler extends TypedHandlerDelegate {
  private static final Logger LOG = Logger.getInstance(GspTypedHandler.class);

  @Override
  public @NotNull Result beforeCharTyped(char c, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file, @NotNull FileType fileType) {
    if (handleTyping(editor, c, project)) {
      return Result.STOP;
    }
    return Result.CONTINUE;
  }

  private static boolean handleTyping(final Editor editor, final char charTyped, final Project project) {
    if (project == null || !HandlerUtils.canBeInvoked(editor, project)) {
      return false;
    }

    final PsiElement file = HandlerUtils.getPsiFile(editor, project);
    if (!(file instanceof GspFile)) return false;

    int caret = editor.getCaretModel().getOffset();
    final EditorHighlighter highlighter = editor.getHighlighter();
    if (caret < 1) return false;

    HighlighterIterator iterator = highlighter.createIterator(caret - 1);
    IElementType tokenType = iterator.getTokenType();

    CharSequence text = editor.getDocument().getCharsSequence();
    if (tokenType == GspTokenTypes.JSCRIPT_BEGIN) {
      if (GspEditorActionsUtil.areSciptletSeparatorsUnbalanced(highlighter.createIterator(caret - 1))) {
        if ('=' == charTyped) {
          return handleJspLikeEqualTyped(editor, text, caret);
        }
        if ('@' == charTyped) {
          return handleJspLikeDirectiveTyped(editor, text, caret);
        }
        if ('!' == charTyped) {
          return handleJspLikeDeclarationTyped(editor, text, caret);
        }
      }
    }

    if ('/' == charTyped && tokenType == XmlTokenType.XML_TAG_NAME && caret == iterator.getEnd()) {
      return handleSlash(editor, text, caret, iterator);
    }

    if ('{' == charTyped) {
      if (tokenType == XmlTokenType.XML_DATA_CHARACTERS || JavaScriptIntegrationUtil.isJSElementType(tokenType)) {
        return handleGspLeftBraceTyped(editor, text, caret, false, project);
      }

      if (tokenType == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN) {
        return handleGspLeftBraceTyped(editor, text, caret, true, project);
      }

      if (tokenType == GspTokenTypes.GSTRING_DOLLAR) {
        iterator.advance();
        if (iterator.getTokenType() == GspTokenTypes.GSP_ATTRIBUTE_VALUE_TOKEN) {
          return handleGspLeftBraceTyped(editor, text, caret, true, project);
        }
      }
    }

    if ('}' == charTyped) {
      return handleGspRightBraceTyped(editor, text, caret);
    }

    if ('[' == charTyped) {
      if (tokenType == XmlTokenType.XML_ATTRIBUTE_VALUE_START_DELIMITER) {
        iterator.advance();
        if (iterator.getTokenType() == XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER) {
          EditorModificationUtil.insertStringAtCaret(editor, "[]");
          editor.getCaretModel().moveCaretRelatively(-1, 0, false, false, true);
          return true;
        }
      }
    }

    return false;
  }

  private static boolean handleSlash(Editor editor, CharSequence text, int caret, HighlighterIterator iterator) {
    for (int i = iterator.getStart(); i < caret; i++) {
      if (text.charAt(i) == ':') {
        if ("tmpl".contentEquals(text.subSequence(iterator.getStart(), i))) {
          return false;
        }

        if (caret < text.length() && text.charAt(caret) == '>') {
          EditorModificationUtil.insertStringAtCaret(editor, "/");
        }
        else {
          EditorModificationUtil.insertStringAtCaret(editor, "/>");
        }

        return true;
      }
    }

    return false;
  }

  private static boolean handleGspRightBraceTyped(Editor editor, CharSequence text, int caret) {
    if (caret == 0 || text.length() < 2 || text.length() < caret + 1) {
      return false;
    }
    if (mustNotPlaceBrace(editor, caret)) {
      editor.getCaretModel().moveCaretRelatively(1, 0, false, false, true);
      return true;
    }
    return false;
  }

  private static boolean mustNotPlaceBrace(Editor editor, int caret) {
    String text = editor.getDocument().getText();
    final EditorHighlighter highlighter = editor.getHighlighter();
    if (caret < 1) return false;
    HighlighterIterator iterator = highlighter.createIterator(caret);
    return text.charAt(caret) == '}' && iterator.getTokenType() == GEXPR_END;
  }

  private static boolean handleGspLeftBraceTyped(final Editor editor, final CharSequence text, final int caret, boolean inGrailsTagArgValue,
                                                 final Project project) {
    if (caret < 1 || text.length() < Math.min(caret - 1, 1)) {
      return false;
    }
    EditorActionManager manager = EditorActionManager.getInstance();
    EditorActionHandler handler = manager.getActionHandler(IdeActions.ACTION_EDITOR_ENTER);

    LOG.assertTrue(project != null);

    if (text.charAt(caret - 1) == '$') {
      if (text.length() > caret && text.charAt(caret) == '}') {
        return false;
      }
      EditorModificationUtil.insertStringAtCaret(editor, "{}"); //GSP-like expression
      editor.getCaretModel().moveCaretRelatively(-1, 0, false, false, true);
      return true;
    }
    if (!inGrailsTagArgValue && text.charAt(caret - 1) == '@') { //GSP-like directive
      if (text.length() > caret && text.charAt(caret) == '}') {
        return false;
      }
      EditorModificationUtil.insertStringAtCaret(editor, "{  }");
      editor.getCaretModel().moveCaretRelatively(-2, 0, false, false, true);
      return true;
    }
    if (!inGrailsTagArgValue && text.charAt(caret - 1) == '!') {  //GSP-like declaration
      if (text.length() > caret && text.charAt(caret) == '}') {
        return false;
      }
      EditorModificationUtil.insertStringAtCaret(editor, "{}!");
      editor.getCaretModel().moveCaretRelatively(-2, 0, false, false, true);
      PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
      handler.execute(editor, editor.getCaretModel().getCurrentCaret(),
                      DataManager.getInstance().getDataContext(editor.getContentComponent()));
      GspEditorActionsUtil.insertSpacesByGspIndent(editor, project);
      return true;
    }
// removed according to GRVY-1519
/* 
    if (text.charAt(caret - 1) == '%' && !inGrailsTagArgValue) {  //GSP-like code injection
      if (text.length() > caret && text.charAt(caret) == '}') {
        return false;
      }
      EditorModificationUtil.insertStringAtCaret(editor, "{}%");
      editor.getCaretModel().moveCaretRelatively(-2, 0, false, false, true);
      PsiDocumentManager.getInstance(myProject).commitDocument(editor.getDocument());
      handler.execute(editor, dataContext);
      GspEditorActionsUtil.insertSpacesByGspIndent(editor, dataContext);
      return true;
    }
*/
    return false;
  }

  /**
   * Inserts JSP-like expression injection ending
   */
  private static boolean handleJspLikeEqualTyped(final Editor editor, final CharSequence text, final int caret) {
    if (caret < 2 || text.length() < Math.min(caret - 2, 2)) {
      return false;
    }
    if (text.charAt(caret - 1) == '%' && text.charAt(caret - 2) == '<') {
      if (text.length() > caret && text.charAt(caret) == '%') {
        return false;
      }
      EditorModificationUtil.insertStringAtCaret(editor, "=  %>");
      editor.getCaretModel().moveCaretRelatively(-3, 0, false, false, true);
      return true;
    }
    return false;
  }

  /**
   * Inserts JSP-like directive ending
   */
  private static boolean handleJspLikeDirectiveTyped(final Editor editor, final CharSequence text, final int caret) {
    if (caret < 2 || text.length() < Math.min(caret - 2, 2)) {
      return false;
    }
    if (text.charAt(caret - 1) == '%' && text.charAt(caret - 2) == '<') {
      if (text.length() > caret && text.charAt(caret) == '%') {
        return false;
      }
      EditorModificationUtil.insertStringAtCaret(editor, "@  %>");
      editor.getCaretModel().moveCaretRelatively(-3, 0, false, false, true);
      return true;
    }
    return false;
  }

  /**
   * Inserts JSP-like declaration ending
   */
  private static boolean handleJspLikeDeclarationTyped(final Editor editor, final CharSequence text, final int caret) {
    if (caret < 2 || text.length() < Math.min(caret - 2, 2)) {
      return false;
    }
    if (text.charAt(caret - 1) == '%' && text.charAt(caret - 2) == '<') {
      if (text.length() > caret && text.charAt(caret) == '%') {
        return false;
      }
      EditorModificationUtil.insertStringAtCaret(editor, "!  %>");
      editor.getCaretModel().moveCaretRelatively(-3, 0, false, false, true);
      return true;
    }
    return false;
  }

}
