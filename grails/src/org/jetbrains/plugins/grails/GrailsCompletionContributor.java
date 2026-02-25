// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifier;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GrGspDeclarationHolder;
import org.jetbrains.plugins.grails.references.domain.GormDynamicFinderCompletionProvider;
import org.jetbrains.plugins.grails.references.domain.GormFetchModeCompletionProvider;
import org.jetbrains.plugins.groovy.lang.completion.GroovyCompletionData;
import org.jetbrains.plugins.groovy.lang.completion.GroovyCompletionUtil;
import org.jetbrains.plugins.groovy.lang.completion.impl.FastGroovyCompletionConsumer;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyTokenTypes;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariableDeclaration;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.types.GrClassTypeElement;
import org.jetbrains.plugins.groovy.lang.psi.api.types.GrCodeReferenceElement;

/**
 * @author Maxim.Medvedev
 */
public final class GrailsCompletionContributor extends CompletionContributor {

  public static final PsiElementPattern.Capture<PsiElement> grFieldNamePattern = PlatformPatterns.psiElement(GroovyTokenTypes.mIDENT).withParent(
    StandardPatterns.or(
      PlatformPatterns.psiElement(GrField.class),
      PlatformPatterns.psiElement(GrCodeReferenceElement.class).withParent(
        PlatformPatterns.psiElement(GrClassTypeElement.class).withParent(GrVariableDeclaration.class))
    )
  );

  //public static final PsiElementPattern.Capture<PsiElement> grStaticFieldNamePattern = psiElement(GroovyTokenTypes.mIDENT).withParent(
  //  or(
  //    PsiJavaPatterns.psiField().withModifiers(PsiModifier.STATIC),
  //    psiElement(GrCodeReferenceElement.class).withParent(psiElement(GrClassTypeElement.class).withParent(PsiJavaPatterns.psiVariable().withModifiers(PsiModifier.STATIC)))
  //  )
  //);
  //
  private static final PsiElementPattern.Capture<PsiElement> grReferencePattern =
    PlatformPatterns.psiElement().withParent(GrReferenceExpression.class);

  public GrailsCompletionContributor() {
    extend(CompletionType.BASIC, grReferencePattern, new GormDynamicFinderCompletionProvider());

    extend(CompletionType.BASIC, PlatformPatterns.psiElement(GroovyTokenTypes.mIDENT).withParent(
      PsiJavaPatterns.psiField().withModifiers(PsiModifier.STATIC)), new GrailsStaticFieldCompletionProvider(true));

    extend(CompletionType.BASIC, grFieldNamePattern, new GrailsStaticFieldCompletionProvider(false));

    extend(CompletionType.BASIC, grFieldNamePattern, new GrailsPluginFieldCompletionProvider());

    extend(CompletionType.BASIC, PsiJavaPatterns.psiField().withModifiers(PsiModifier.STATIC), new GrailsPluginFieldCompletionProvider());

    extend(CompletionType.BASIC, PlatformPatterns
      .psiElement().withParent(GrGspDeclarationHolder.class), new CompletionProvider<>() {
      @Override
      protected void addCompletions(@NotNull CompletionParameters parameters,
                                    @NotNull ProcessingContext context,
                                    @NotNull CompletionResultSet result) {
        if (GroovyCompletionUtil.isNewStatement(parameters.getPosition(), false)) {
          try (FastGroovyCompletionConsumer consumer = new FastGroovyCompletionConsumer(result)) {
            GroovyCompletionData.addKeywords(consumer, true, PsiModifier.STATIC, PsiModifier.FINAL);
            GroovyCompletionData.addModifiers(parameters.getPosition(), consumer);
            GroovyCompletionData.addKeywords(consumer, true, GroovyCompletionData.BUILT_IN_TYPES);
          }
        }
      }
    });

    GormFetchModeCompletionProvider.register(this);
  }

}
