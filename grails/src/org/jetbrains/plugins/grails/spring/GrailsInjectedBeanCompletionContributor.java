// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.spring;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.spring.CommonSpringModel;
import com.intellij.spring.model.SpringBeanPointer;
import com.intellij.spring.model.converters.SpringConverterUtil;
import com.intellij.spring.model.utils.SpringModelUtils;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.GrailsCompletionContributor;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.GrModifier;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariableDeclaration;
import org.jetbrains.plugins.groovy.lang.psi.api.types.GrClassTypeElement;
import org.jetbrains.plugins.groovy.lang.psi.api.types.GrCodeReferenceElement;

import java.util.HashSet;
import java.util.Set;

public final class GrailsInjectedBeanCompletionContributor extends CompletionContributor {

  public GrailsInjectedBeanCompletionContributor() {
    extend(CompletionType.BASIC, GrailsCompletionContributor.grFieldNamePattern, new CompletionProvider<>() {
      @Override
      protected void addCompletions(@NotNull CompletionParameters parameters,
                                    @NotNull ProcessingContext context,
                                    @NotNull CompletionResultSet result) {

        PsiElement parent = parameters.getPosition().getParent();
        if (parent instanceof GrField) {
          if (((GrField)parent).getDeclaredType() != null) return;
        }
        else {
          assert parent instanceof GrCodeReferenceElement;
          PsiElement parent2 = parent.getParent();
          assert parent2 instanceof GrClassTypeElement;
          PsiElement variableDeclaration = parent2.getParent();
          assert variableDeclaration instanceof GrVariableDeclaration;
          if (!((GrVariableDeclaration)variableDeclaration).hasModifierProperty(GrModifier.DEF)) return;
        }

        PsiClass aClass = PsiTreeUtil.getParentOfType(parent, PsiClass.class);

        if (aClass == null) return;

        if (!InjectedSpringBeanProvider.isSupportInjection(aClass)) return;

        final CommonSpringModel springModel = SpringModelUtils.getInstance().getSpringModel(aClass);

        Set<SpringBeanPointer> beans = new HashSet<>(springModel.getAllCommonBeans());

        for (PsiField psiField : aClass.getFields()) {
          if (psiField instanceof GrField) {
            beans.remove(InjectedSpringBeanProvider.getInjectedBean(psiField));
          }
        }

        for (SpringBeanPointer beanPointer : beans) {
          LookupElement lookupElement = SpringConverterUtil.createCompletionVariant(beanPointer);
          if (lookupElement != null) {
            result.addElement(lookupElement);
          }
        }
      }
    });
  }
}
