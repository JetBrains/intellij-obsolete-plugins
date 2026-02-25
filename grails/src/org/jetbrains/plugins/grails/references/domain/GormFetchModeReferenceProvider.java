// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyPatterns.grField;
import static org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyPatterns.namedArgumentLabel;

public class GormFetchModeReferenceProvider extends PsiReferenceProvider {

  public static void register(PsiReferenceRegistrar registrar) {
    GormFetchModeReferenceProvider provider = new GormFetchModeReferenceProvider();

    registrar.registerReferenceProvider(
      namedArgumentLabel(null).withParent(psiElement(GrNamedArgument.class).withParent(psiElement(GrListOrMap.class).withParent(
          grField().withName("fetchMode").withModifiers(PsiModifier.STATIC))
      )),
      provider
    );
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    PsiElement namedArgument = element.getParent();
    PsiElement listOrMap = namedArgument.getParent();
    GrField field = (GrField)listOrMap.getParent();

    PsiClass domainClass = field.getContainingClass();
    if (domainClass == null) return PsiReference.EMPTY_ARRAY;
    if (!GormUtils.isGormBean(domainClass)) return PsiReference.EMPTY_ARRAY;

    return new PsiReference[]{
      new GormPropertyReference(element, false, domainClass)
    };
  }
}
