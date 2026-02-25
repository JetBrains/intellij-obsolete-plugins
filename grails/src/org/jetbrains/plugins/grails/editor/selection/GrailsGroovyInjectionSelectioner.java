// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.editor.selection;

import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.PsiImplUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.lexer.core.GspTokenTypes;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GrGspExprInjection;

import java.util.Collections;
import java.util.List;

public final class GrailsGroovyInjectionSelectioner extends ExtendWordSelectionHandlerBase {
  @Override
  public boolean canSelect(@NotNull PsiElement e) {
    return e instanceof GrGspExprInjection;
  }

  @Override
  public List<TextRange> select(@NotNull PsiElement element, @NotNull CharSequence editorText, int cursorOffset, @NotNull Editor editor) {
    PsiElement prev = element.getPrevSibling();
    PsiElement next = element.getNextSibling();

    if (PsiImplUtil.isLeafElementOfType(prev, GspTokenTypes.GEXPR_BEGIN) && PsiImplUtil.isLeafElementOfType(next, GspTokenTypes.GEXPR_END)
      || PsiImplUtil.isLeafElementOfType(prev, GspTokenTypes.JEXPR_BEGIN) && PsiImplUtil.isLeafElementOfType(next, GspTokenTypes.JEXPR_END)
      ) {
      assert next != null;
      assert prev != null;
      return Collections.singletonList(new TextRange(prev.getTextOffset(), next.getTextRange().getEndOffset()));
    }

    return Collections.emptyList();
  }
}
