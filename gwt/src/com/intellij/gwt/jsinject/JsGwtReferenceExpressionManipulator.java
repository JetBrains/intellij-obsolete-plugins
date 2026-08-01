/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.gwt.jsinject;

import com.intellij.gwt.jsinject.parser.GwtLanguageDialect;
import com.intellij.lang.ASTNode;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.impl.JSChangeUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

public final class JsGwtReferenceExpressionManipulator extends AbstractElementManipulator<JSGwtReferenceExpressionImpl> {
  private static final Logger LOG = Logger.getInstance(JsGwtReferenceExpressionManipulator.class);

  @Override
  public JSGwtReferenceExpressionImpl handleContentChange(final @NotNull JSGwtReferenceExpressionImpl element,
                                                          final @NotNull TextRange range, final String newContent)
    throws IncorrectOperationException {
    String newText = range.replace(element.getText(), newContent);
    ASTNode callExpression = JSChangeUtil.createExpressionFromText(element.getProject(), newText + "()", GwtLanguageDialect.GWT_DIALECT, false);
    PsiElement referenceExpression = callExpression.getPsi(JSCallExpression.class).getMethodExpression();
    LOG.assertTrue(referenceExpression != null, newText);
    if (!(referenceExpression instanceof JSGwtReferenceExpressionImpl)) {
      final PsiElement[] children = referenceExpression.getChildren();
      if (children.length == 1) {
        referenceExpression = children[0];
      }
    }
    final PsiElement newElement = element.replace(referenceExpression);
    if (!(newElement instanceof JSGwtReferenceExpressionImpl)) {
      LOG.error("Cannot rename injected GWT reference, newText=" + newText);
    }
    return (JSGwtReferenceExpressionImpl)newElement;
  }

  @Override
  public @NotNull TextRange getRangeInElement(final @NotNull JSGwtReferenceExpressionImpl element) {
    String text = element.getText();
    int start = text.indexOf('@');
    if (start == -1) start = 0;
    int end = text.indexOf("::");
    if (end == -1) end = text.length();
    return new TextRange(start, end);
  }
}
