package com.intellij.gwt.jsinject;

import com.intellij.lang.javascript.JSTokenTypes;
import com.intellij.lang.javascript.psi.JSAssignmentExpression;
import com.intellij.lang.javascript.psi.JSDefinitionExpression;
import com.intellij.lang.javascript.psi.JSParenthesizedExpression;
import com.intellij.lang.javascript.psi.JSPostfixExpression;
import com.intellij.lang.javascript.psi.JSPrefixExpression;
import com.intellij.lang.javascript.psi.JSReferenceExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.psi.util.PsiTreeUtil.getParentOfType;
import static com.intellij.psi.util.PsiTreeUtil.skipParentsOfType;

/**
 * That's basically a partial ripoff of {@link com.intellij.psi.util.PsiUtil}
 */
public final class JsPsiUtil {

  public static boolean isAccessedForWriting(@NotNull JSGwtReferenceExpressionImpl expr) {
    PsiElement parent = skipParentsOfType(expr, JSParenthesizedExpression.class);
    if (isOnAssignmentLeftHand(parent)) return true;
    if (parent instanceof JSReferenceExpression) {
      parent = parent.getParent();
      if (parent instanceof JSPrefixExpression) {
        IElementType tokenType = ((JSPrefixExpression) parent).getOperationSign();
        return tokenType == JSTokenTypes.PLUSPLUS || tokenType == JSTokenTypes.MINUSMINUS;
      }
      if (parent instanceof JSPostfixExpression) {
        IElementType tokenType = ((JSPostfixExpression) parent).getOperationSign();
        return tokenType == JSTokenTypes.PLUSPLUS || tokenType == JSTokenTypes.MINUSMINUS;
      }
    }
    return false;
  }

  public static boolean isAccessedForReading(@NotNull JSGwtReferenceExpressionImpl expr) {
    PsiElement parent = skipParentsOfType(expr, JSParenthesizedExpression.class);
    //noinspection ConstantConditions
    return !isOnAssignmentLeftHand(parent) ||
           getParentOfType(parent, JSAssignmentExpression.class).getOperationSign() != JSTokenTypes.EQ;
  }

  private static boolean isOnAssignmentLeftHand(@Nullable PsiElement expr) {
    if (expr instanceof JSReferenceExpression) {
      PsiElement definition = expr.getParent();
      if (definition instanceof JSDefinitionExpression) {
        PsiElement assignment = definition.getParent();
        if (assignment instanceof JSAssignmentExpression) {
          return true;
        }
      }
    }
    return false;
  }
}
