// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.intentions;

import com.intellij.modcommand.ActionContext;
import com.intellij.modcommand.Presentation;
import com.intellij.modcommand.PsiUpdateModCommandAction;
import com.intellij.psi.PsiMethodCallExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base of the intentions that rewrite a Java {@code bind(...)...} chain.
 *
 * <p>The platform climbs from the caret to the enclosing calls and takes the first one that the
 * {@link PsiElementPredicate} accepts; the predicates accept only the outermost call of a chain, so the
 * intention is available on every part of {@code bind(Foo.class).to(FooImpl.class).in(Scopes.SINGLETON)}.
 * {@link #invoke} runs on a writable copy and takes further writable elements, such as a class in
 * another file, from the updater, which also gives the preview.
 */
abstract class BindingChainIntention extends PsiUpdateModCommandAction<PsiMethodCallExpression> {
  private final @NotNull PsiElementPredicate myPredicate;
  private final @NotNull String myText;

  BindingChainIntention(@NotNull PsiElementPredicate predicate, @NotNull String text) {
    super(PsiMethodCallExpression.class);
    myPredicate = predicate;
    myText = text;
  }

  @Override
  protected boolean isElementApplicable(@NotNull PsiMethodCallExpression element, @NotNull ActionContext context) {
    return myPredicate.satisfiedBy(element);
  }

  @Override
  protected @Nullable Presentation getPresentation(@NotNull ActionContext context, @NotNull PsiMethodCallExpression element) {
    return Presentation.of(myText);
  }
}
