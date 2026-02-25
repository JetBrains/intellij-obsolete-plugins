// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.highlighter;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.editor.impl.EditorHighlighterCache;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.search.IndexPatternBuilder;
import com.intellij.psi.impl.search.LexerEditorHighlighterLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.lexer.GspTokenTypesEx;
import org.jetbrains.plugins.grails.lang.gsp.lexer.core.GspTokenTypes;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspFile;
import org.jetbrains.plugins.groovy.lang.groovydoc.parser.GroovyDocElementTypes;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyTokenTypes;

public final class GspIndexPatternBuilder implements IndexPatternBuilder {
  @Override
  public Lexer getIndexingLexer(@NotNull PsiFile file) {
    if (file instanceof GspFile) {
      EditorHighlighter highlighter;

      final Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
      EditorHighlighter cachedEditorHighlighter;
      boolean alreadyInitializedHighlighter = false;

      if ((cachedEditorHighlighter = EditorHighlighterCache.getEditorHighlighterForCachesBuilding(document)) != null &&
          EditorHighlighterCache.checkCanUseCachedEditorHighlighter(file.getText(), cachedEditorHighlighter)) {
        highlighter = cachedEditorHighlighter;
        alreadyInitializedHighlighter = true;
      } else {
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) return null;

        highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(file.getProject(), virtualFile);
      }

      return new LexerEditorHighlighterLexer(highlighter, alreadyInitializedHighlighter);
    }
    return null;
  }

  @Override
  public TokenSet getCommentTokenSet(@NotNull PsiFile file) {
    return GspTokenTypesEx.GSP_COMMENTS;
  }

  @Override
  public int getCommentStartDelta(IElementType tokenType) {
    if (tokenType == GspTokenTypes.JSP_STYLE_COMMENT || tokenType == GspTokenTypes.GSP_STYLE_COMMENT) {
      return 4;
    }

    return 0;
  }

  @Override
  public int getCommentEndDelta(IElementType tokenType) {
    if (tokenType == GspTokenTypes.JSP_STYLE_COMMENT || tokenType == GspTokenTypes.GSP_STYLE_COMMENT) {
      return 4;
    }

    if (tokenType == GroovyTokenTypes.mML_COMMENT || tokenType == GroovyDocElementTypes.GROOVY_DOC_COMMENT) {
      return 2;
    }

    return 0;
  }
}
