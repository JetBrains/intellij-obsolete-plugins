// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.util.InspectionMessage;
import com.intellij.guice.model.extensions.GuiceCallMatcher;
import com.intellij.guice.model.extensions.GuiceCallPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.uast.UastHintedVisitorAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public abstract class BaseUastInspection extends LocalInspectionTool {
  protected abstract @NotNull @InspectionMessage String buildErrorString(Object... infos);

  protected boolean buildQuickFixesOnlyForOnTheFlyErrors() {
    return false;
  }

  private final Class<? extends UElement>[] myUElementsTypesHint;
  private @Nullable GuiceCallMatcher.Builder<BiConsumer<UCallExpression, BaseUastInspectionVisitor>> myCallMatcherBuilder;
  private volatile @Nullable GuiceCallMatcher<BiConsumer<UCallExpression, BaseUastInspectionVisitor>> myCompiledCallMatcher;
  private volatile Class<? extends UElement> @Nullable [] myEffectiveHints;

  @SafeVarargs
  protected BaseUastInspection(Class<? extends UElement>... uElementsTypesHint) {
    myUElementsTypesHint = uElementsTypesHint;
  }

  /**
   * Registers a declarative call rule that is compiled into a multi-stage candidate filter
   * and evaluated in {@link #buildUastVisitor(ProblemsHolder, boolean)}.
   */
  protected final void extendCall(@NotNull GuiceCallPattern pattern,
                                  @NotNull BiConsumer<@NotNull UCallExpression, @NotNull BaseUastInspectionVisitor> handler) {
    if (myCallMatcherBuilder == null) {
      myCallMatcherBuilder = GuiceCallMatcher.builder(false);
    }
    myCallMatcherBuilder.add(pattern, handler);
    myCompiledCallMatcher = null;
    myEffectiveHints = null;
  }

  private @NotNull GuiceCallMatcher<BiConsumer<UCallExpression, BaseUastInspectionVisitor>> getCompiledCallMatcher() {
    GuiceCallMatcher<BiConsumer<UCallExpression, BaseUastInspectionVisitor>> compiled = myCompiledCallMatcher;
    if (compiled == null) {
      GuiceCallMatcher.Builder<BiConsumer<UCallExpression, BaseUastInspectionVisitor>> builder = myCallMatcherBuilder;
      compiled = builder != null ? builder.build() : GuiceCallMatcher.<BiConsumer<UCallExpression, BaseUastInspectionVisitor>>builder(false).build();
      myCompiledCallMatcher = compiled;
    }
    return compiled;
  }

  @SuppressWarnings("unchecked")
  private Class<? extends UElement> @NotNull [] getEffectiveHints() {
    Class<? extends UElement>[] hints = myEffectiveHints;
    if (hints == null) {
      if (myCallMatcherBuilder == null) {
        hints = myUElementsTypesHint;
      }
      else {
        Set<Class<? extends UElement>> merged = new LinkedHashSet<>(Arrays.asList(myUElementsTypesHint));
        merged.add(UCallExpression.class);
        hints = merged.toArray(new Class[0]);
      }
      myEffectiveHints = hints;
    }
    return hints;
  }

  public @NotNull AbstractUastNonRecursiveVisitor buildUastVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    GuiceCallMatcher<BiConsumer<UCallExpression, BaseUastInspectionVisitor>> matcher = getCompiledCallMatcher();
    return new BaseUastInspectionVisitor(this, holder, isOnTheFly) {
      @Override
      public boolean visitCallExpression(@NotNull UCallExpression expression) {
        matcher.process(expression, this, (handler, call, _qName, _cls, visitor) -> {
          handler.accept(call, visitor);
          return true;
        });
        return true;
      }
    };
  }

  @Override
  public @NotNull PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, final boolean isOnTheFly) {
    return UastHintedVisitorAdapter.create(
      holder.getFile().getLanguage(),
      buildUastVisitor(holder, isOnTheFly),
      getEffectiveHints()
    );
  }

  public LocalQuickFix buildFix(PsiElement location, Object[] infos) {
    return null;
  }
}
