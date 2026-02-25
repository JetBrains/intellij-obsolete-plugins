// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.spring;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.spring.SpringManager;
import com.intellij.spring.contexts.model.SpringModel;
import com.intellij.spring.model.SpringBeanPointer;
import com.intellij.spring.model.utils.SpringModelSearchers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.config.GrailsStructure;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightVariable;
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

final class GrailsWebApplicationContextMembersContributor extends NonCodeMembersContributor {
  @Override
  public String getParentClassName() {
    return "org.springframework.context.ApplicationContext";
  }

  @Override
  public void processDynamicElements(@NotNull PsiType qualifierType,
                                     @Nullable PsiClass aClass,
                                     @NotNull PsiScopeProcessor processor,
                                     @NotNull PsiElement place,
                                     @NotNull ResolveState state) {
    if (!ResolveUtil.shouldProcessProperties(processor.getHint(ElementClassHint.KEY))) return;

    GrailsStructure structure = GrailsStructure.getInstance(place);
    if (structure == null) return;

    Module module = structure.getModule();

    final SpringModel model = SpringManager.getInstance(module.getProject()).getCombinedModel(module);

    PsiManager manager = structure.getManager();

    String nameHint = ResolveUtil.getNameHint(processor);

    if (nameHint == null) {
      for (SpringBeanPointer<?> pointer : model.getAllCommonBeans()) {
        if (pointer.isValid()) {
          PsiType type = TypesUtil.getLeastUpperBound(pointer.getEffectiveBeanTypes().toArray(PsiType.EMPTY_ARRAY), manager);
          PsiElement psiElement = pointer.getPsiElement();
          if (psiElement != null) {
            if (!processor.execute(new GrLightVariable(manager, pointer.getName(), type, psiElement), state)) return;
          }
        }
      }
    }
    else {
      SpringBeanPointer<?>  bean = SpringModelSearchers.findBean(model, nameHint);
      if (bean != null && bean.isValid()) {
        PsiType type = TypesUtil.getLeastUpperBound(bean.getEffectiveBeanTypes().toArray(PsiType.EMPTY_ARRAY), manager);
        PsiElement psiElement = bean.getPsiElement();
        if (psiElement != null) {
          if (!processor.execute(new GrLightVariable(manager, nameHint, type, psiElement), state)) return;
        }
      }
    }
  }
}
