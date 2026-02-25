// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.Pair;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.IconManager;
import com.intellij.ui.PlatformIcons;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.completion.handlers.NamedArgumentInsertHandler;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyTokenTypes;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentLabel;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyPatterns;

import java.util.Map;

public final class GormFetchModeCompletionProvider extends CompletionProvider<CompletionParameters> {
  public static void register(CompletionContributor contributor) {
    GormFetchModeCompletionProvider instance = new GormFetchModeCompletionProvider();

    ElementPattern<PsiElement> p1 = PlatformPatterns
      .psiElement(GroovyTokenTypes.mIDENT).withParent(PlatformPatterns.psiElement(GrArgumentLabel.class).withParent(
      PlatformPatterns.psiElement(GrNamedArgument.class).withParent(PlatformPatterns.psiElement(GrListOrMap.class).withParent(
          GroovyPatterns.grField().withName("fetchMode").withModifiers(PsiModifier.STATIC)
    ))));

    ElementPattern<PsiElement> p2 = PlatformPatterns
      .psiElement(GroovyTokenTypes.mIDENT).withParent(PlatformPatterns.psiElement(GrReferenceExpression.class).withParent(
      PlatformPatterns.psiElement(GrListOrMap.class).withParent(
          GroovyPatterns.grField().withName("fetchMode").withModifiers(PsiModifier.STATIC)
    )));

    contributor.extend(CompletionType.BASIC, p1, instance);
    contributor.extend(CompletionType.BASIC, p2, instance);
    contributor.extend(CompletionType.SMART, p1, instance);
    contributor.extend(CompletionType.SMART, p2, instance);
  }

  @Override
  protected void addCompletions(@NotNull CompletionParameters parameters,
                                @NotNull ProcessingContext context,
                                @NotNull CompletionResultSet result) {
    PsiElement element = parameters.getOriginalPosition();
    GrField field = PsiTreeUtil.getParentOfType(element, GrField.class);
    if (field == null) return;

    PsiClass domainClass = field.getContainingClass();
    if (!GormUtils.isGormBean(domainClass)) return;
    assert domainClass != null;

    DomainDescriptor descriptor = DomainDescriptor.getDescriptor(domainClass);

    for (Map.Entry<String, Pair<PsiType, PsiElement>> entry : descriptor.getPersistentProperties().entrySet()) {
      if (descriptor.isToManyRelation(entry.getKey())) {
        LookupElement lookup = LookupElementBuilder.create(entry.getKey())
          .withIcon(IconManager.getInstance().getPlatformIcon(PlatformIcons.Property))
          .withInsertHandler(NamedArgumentInsertHandler.INSTANCE);

        result.addElement(PrioritizedLookupElement.withPriority(lookup, 1));
      }
    }
  }
}
