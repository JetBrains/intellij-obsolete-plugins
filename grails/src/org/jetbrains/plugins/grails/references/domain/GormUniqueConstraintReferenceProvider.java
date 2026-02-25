// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.constraints.GrailsConstraintsUtil;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

import static com.intellij.patterns.PsiJavaPatterns.psiElement;
import static org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyPatterns.namedArgument;
import static org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyPatterns.stringLiteral;

public class GormUniqueConstraintReferenceProvider extends PsiReferenceProvider {


  public static void register(PsiReferenceRegistrar registrar) {
    GormUniqueConstraintReferenceProvider provider = new GormUniqueConstraintReferenceProvider();

    registrar.registerReferenceProvider(
      stringLiteral().withParent(psiElement(GrListOrMap.class).withParent(namedArgument().withLabel("unique"))), provider);

    registrar.registerReferenceProvider(stringLiteral().withParent(namedArgument().withLabel("unique")), provider);
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    PsiElement parent = element.getParent();

    if (parent instanceof GrListOrMap listOrMap) {
      if (listOrMap.isMap()) return PsiReference.EMPTY_ARRAY;

      parent = listOrMap.getParent();
    }

    if (!(parent instanceof GrNamedArgument namedArgument)) return PsiReference.EMPTY_ARRAY;

    if (!"unique".equals(namedArgument.getLabelName())) return PsiReference.EMPTY_ARRAY;

    // Optimization.
    GrField field = PsiTreeUtil.getParentOfType(namedArgument, GrField.class);
    if (field == null || !"constraints".equals(field.getName()) || !field.hasModifierProperty(PsiModifier.STATIC)) return PsiReference.EMPTY_ARRAY;

    GrCall methodCall = PsiUtil.getCallByNamedParameter(namedArgument);
    if (methodCall == null) return PsiReference.EMPTY_ARRAY;

    final PsiMethod method = methodCall.resolveMethod();
    if (!GrailsConstraintsUtil.isConstraintsMethod(method)) return PsiReference.EMPTY_ARRAY;

    PsiClass validatedClass = GrailsConstraintsUtil.getValidatedClass(method);
    if (!GormUtils.isGormBean(validatedClass)) return PsiReference.EMPTY_ARRAY;

    return new PsiReference[]{new GormPropertyReferenceUnique(element, false, validatedClass) {
      @Override
      protected boolean isValidForCompletion(String fieldName, PsiType type, DomainDescriptor descriptor) {
        //noinspection ConstantConditions
        return super.isValidForCompletion(fieldName, type,descriptor) 
               && !fieldName.equals(method.getName())
               && !fieldName.equals("id")
               && !fieldName.equals("version")
               && !descriptor.isToManyRelation(fieldName);
      }
    }};
  }
}
