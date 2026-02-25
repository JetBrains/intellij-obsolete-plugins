// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.TraitInjectorService;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.transformations.AstTransformationSupport;
import org.jetbrains.plugins.groovy.transformations.TransformationContext;

import java.util.Collection;

final class Grails3TraitInjectorContributor implements AstTransformationSupport {
  @Override
  public void applyTransformation(@NotNull TransformationContext context) {
    GrTypeDefinition clazz = context.getCodeClass();
    final Collection<String> injectedTraitsFQNs = TraitInjectorService.getInjectedTraits(clazz);
    if (injectedTraitsFQNs.isEmpty()) return;
    injectTraits(clazz, context, injectedTraitsFQNs);
  }

  static void injectTraits(GrTypeDefinition clazz, TransformationContext context, Collection<String> injectedTraitsFQNs) {
    final JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(clazz.getProject());
    final PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(clazz.getProject());
    final PsiClassType currentClassType = context.eraseClassType(elementFactory.createType(clazz, PsiSubstitutor.EMPTY));
    for (String fqn : injectedTraitsFQNs) {
      final PsiClass traitClass = psiFacade.findClass(fqn, clazz.getResolveScope());
      if (traitClass == null) continue;
      final PsiTypeParameter[] traitTypeParameters = traitClass.getTypeParameters();
      final PsiType[] types = traitTypeParameters.length == 0
                              ? PsiType.EMPTY_ARRAY
                              : ContainerUtil.map2Array(traitTypeParameters, PsiType.class,
                                                        typeParameter -> currentClassType);
      context.addInterface(elementFactory.createType(traitClass, types));
    }
  }
}
