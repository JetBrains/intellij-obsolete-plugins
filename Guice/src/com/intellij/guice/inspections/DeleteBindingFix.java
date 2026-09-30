// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.guice.utils.MutationUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UQualifiedReferenceExpression;
import org.jetbrains.uast.UastUtils;

/**
 * Deletes a binding or a part of it. The {@link Mode} tells which part.
 */
final class DeleteBindingFix implements LocalQuickFix {
  private static final Logger LOGGER = Logger.getInstance(DeleteBindingFix.class);

  enum Mode {
    /** Removes the reported call from the end of a chain: {@code bind(A.class).to(A.class)} becomes {@code bind(A.class)}. */
    CHAIN_CALL,
    /** Deletes the statement that holds the reported call, with a qualifier such as {@code binder().}. */
    STATEMENT,
    /**
     * Deletes the reported argument if the call has other arguments.
     * If the argument is the only one, deletes the statement.
     */
    ARGUMENT,
  }

  private final Mode myMode;

  DeleteBindingFix(@NotNull Mode mode) {
    myMode = mode;
  }

  @Override
  public @NotNull String getName() {
    return GuiceBundle.message("delete.binding");
  }

  @Override
  public @NotNull String getFamilyName() {
    return GuiceBundle.message("delete.binding");
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    PsiElement element = descriptor.getPsiElement();
    if (element == null) return;
    UCallExpression call = UastUtils.findContaining(element, UCallExpression.class);
    if (call == null) return;

    try {
      switch (myMode) {
        case CHAIN_CALL -> removeChainCall(call);
        case STATEMENT -> deleteStatement(call);
        case ARGUMENT -> {
          if (call.getValueArgumentCount() > 1) {
            deleteArgument(call, element);
          }
          else {
            deleteStatement(call);
          }
        }
      }
    }
    catch (IncorrectOperationException e) {
      LOGGER.error(e);
    }
  }

  /**
   * In a chain such as {@code bind(...).to(Foo.class)}, the UAST tree is a qualified expression with
   * {@code bind(...)} as the receiver and {@code to(Foo.class)} as the selector.
   * The fix replaces the whole expression with the receiver.
   */
  private static void removeChainCall(@NotNull UCallExpression call) {
    UElement parent = call.getUastParent();
    if (!(parent instanceof UQualifiedReferenceExpression qualified)) return;

    UExpression receiver = qualified.getReceiver();
    PsiElement wholePsi = qualified.getSourcePsi();
    PsiElement receiverPsi = receiver.getSourcePsi();
    if (wholePsi == null || receiverPsi == null) return;

    if (wholePsi instanceof PsiExpression wholeExpression) {
      MutationUtils.replaceExpression(receiverPsi.getText(), wholeExpression);
    }
    else {
      // Kotlin and other languages: a generic PSI replacement.
      wholePsi.replace(receiverPsi.copy());
    }
  }

  /**
   * Deletes the statement that ends with the call. The fix does nothing if the call is part of a larger expression,
   * for example {@code b = bind(Foo.class)}.
   */
  private static void deleteStatement(@NotNull UCallExpression call) {
    UExpression statement = GuiceUtils.getStatementExpression(call);
    PsiElement psi = statement != null ? statement.getSourcePsi() : null;
    if (psi == null) return;

    // Java wraps the expression in a statement. Kotlin has the expression directly in the block.
    PsiExpressionStatement javaStatement = PsiTreeUtil.getParentOfType(psi, PsiExpressionStatement.class, false, PsiCodeBlock.class);
    (javaStatement != null ? javaStatement : psi).delete();
  }

  /**
   * Deletes one argument and one comma next to it.
   */
  private static void deleteArgument(@NotNull UCallExpression call, @NotNull PsiElement element) {
    PsiElement argument = null;
    for (UExpression arg : call.getValueArguments()) {
      PsiElement argPsi = arg.getSourcePsi();
      if (argPsi != null && PsiTreeUtil.isAncestor(argPsi, element, false)) {
        argument = argPsi;
        break;
      }
    }
    if (argument == null) return;

    // Go up to the child of the argument list. Kotlin wraps each argument in a KtValueArgument.
    PsiElement listChild = argument;
    while (listChild.getParent() != null && findComma(listChild, true) == null && findComma(listChild, false) == null) {
      listChild = listChild.getParent();
      if (!PsiTreeUtil.isAncestor(call.getSourcePsi(), listChild, true)) return;
    }

    PsiElement parent = listChild.getParent();
    PsiElement nextComma = findComma(listChild, true);
    if (nextComma != null) {
      PsiElement last = nextComma.getNextSibling() instanceof PsiWhiteSpace space ? space : nextComma;
      parent.deleteChildRange(listChild, last);
      return;
    }
    PsiElement previousComma = findComma(listChild, false);
    if (previousComma != null) {
      parent.deleteChildRange(previousComma, listChild);
    }
  }

  /**
   * Returns the comma that follows or precedes the element, with only white space and comments between them.
   */
  private static @Nullable PsiElement findComma(@NotNull PsiElement element, boolean forward) {
    PsiElement sibling = forward
                         ? PsiTreeUtil.skipWhitespacesAndCommentsForward(element)
                         : PsiTreeUtil.skipWhitespacesAndCommentsBackward(element);
    return sibling != null && ",".equals(sibling.getText()) ? sibling : null;
  }
}
