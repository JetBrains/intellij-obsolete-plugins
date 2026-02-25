// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.editor.selection;

import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrAssignmentExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrStringContent;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrLiteralImpl;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrStringImpl;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;
import org.jetbrains.plugins.groovy.lang.resolve.GroovyStringLiteralManipulator;

import java.util.Collections;
import java.util.List;

public final class GrailsDependencySelectioner extends ExtendWordSelectionHandlerBase {
  @Override
  public boolean canSelect(@NotNull PsiElement e) {
    PsiElement parent = e.getParent();

    if (!(parent instanceof GrLiteralImpl || parent instanceof GrStringImpl && e instanceof GrStringContent)) {
      return false;
    }

    PsiElement argumentList = parent.getParent();
    if (!(argumentList instanceof GrArgumentList)) return false;

    PsiElement methodCall = argumentList.getParent();
    if (!(methodCall instanceof GrMethodCall)) return false;

    GrClosableBlock dependencyClosure = PsiTreeUtil.getParentOfType(methodCall, GrClosableBlock.class);
    if (dependencyClosure == null) return false;

    PsiElement dependencyMethodCall = dependencyClosure.getParent();
    if (dependencyMethodCall instanceof GrArgumentList) {
      dependencyMethodCall = dependencyMethodCall.getParent();
    }

    if (!(dependencyMethodCall instanceof GrMethodCall)) return false;

    String methodName = PsiUtil.getMethodName((GrMethodCall)dependencyMethodCall);

    if (!"plugins".equals(methodName) && !"dependencies".equals(methodName)) return false;

    PsiElement depResolutionClosure = dependencyMethodCall.getParent();
    if (!(depResolutionClosure instanceof GrClosableBlock)) return false;

    PsiElement eAssignmentExpression = depResolutionClosure.getParent();
    if (!(eAssignmentExpression instanceof GrAssignmentExpression assignmentExpression)) return false;

    if (assignmentExpression.getRValue() != depResolutionClosure
        || !"grails.project.dependency.resolution".equals(assignmentExpression.getLValue().getText())) {
      return false;
    }

    if (!GrailsUtils.BUILD_CONFIG.equals(assignmentExpression.getContainingFile().getName())) return false;

    return true;
  }

  @Override
  public List<TextRange> select(@NotNull PsiElement e, @NotNull CharSequence editorText, int cursorOffset, @NotNull Editor editor) {
    TextRange range;

    if (e.getParent() instanceof GrLiteralImpl) {
      String text = e.getText();
      range = GroovyStringLiteralManipulator.getLiteralRange(text).shiftRight(e.getTextRange().getStartOffset());
    }
    else {
      range = e.getTextRange();
    }

    int begin = cursorOffset;
    for (; begin > range.getStartOffset() && editorText.charAt(begin - 1) != ':'; begin--) ;

    int end = cursorOffset;
    for (; end < range.getEndOffset() && editorText.charAt(end) != ':'; end++) ;

    return Collections.singletonList(new TextRange(begin, end));
  }
}
