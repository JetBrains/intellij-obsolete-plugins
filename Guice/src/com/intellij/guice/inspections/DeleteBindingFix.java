// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.utils.MutationUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UQualifiedReferenceExpression;
import org.jetbrains.uast.UastUtils;

class DeleteBindingFix implements LocalQuickFix {
    private static final Logger LOGGER = Logger.getInstance("DeleteBindingFix");

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
        UCallExpression call = UastUtils.findContaining(descriptor.getPsiElement(), UCallExpression.class);
        if (call == null) return;

        // In a chained call like `bind(...).to(Foo.class)`, the UAST tree is:
        //   UQualifiedReferenceExpression
        //     receiver: bind(...)
        //     selector: to(Foo.class)
        // Deleting `.to(Foo.class)` replaces the whole expression with `bind(...)`.
        UElement parent = call.getUastParent();
        if (!(parent instanceof UQualifiedReferenceExpression qre)) {
            // Standalone call (e.g., PointlessBinding: just `bind(Foo.class)`) — delete whole statement.
            PsiElement psi = call.getSourcePsi();
            if (psi != null) {
                try {
                    // Java wraps in PsiExpressionStatement; delete the statement.
                    // Kotlin has the call directly in the block; delete just the call.
                    PsiElement toDelete = psi.getParent() instanceof PsiExpressionStatement
                        ? psi.getParent() : psi;
                    toDelete.delete();
                } catch (IncorrectOperationException e) {
                    LOGGER.error(e);
                }
            }
            return;
        }

        UExpression receiver = qre.getReceiver();
        PsiElement wholePsi = qre.getSourcePsi();
        PsiElement receiverPsi = receiver != null ? receiver.getSourcePsi() : null;
        if (wholePsi == null || receiverPsi == null) return;

        try {
            // Java: use the type-safe replaceExpression helper.
            if (wholePsi instanceof PsiExpression wholeExpr) {
                MutationUtils.replaceExpression(receiverPsi.getText(), wholeExpr);
            } else {
                // Kotlin (and other languages): generic PSI replacement.
                wholePsi.replace(receiverPsi.copy());
            }
        } catch (IncorrectOperationException e) {
            LOGGER.error(e);
        }
    }
}
