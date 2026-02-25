// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain.namedQuery;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.DelegatingScopeProcessor;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.references.domain.DomainMembersProvider;
import org.jetbrains.plugins.grails.references.domain.GormUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.GrModifierFlags;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightField;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

final class NamedCriteriaProxyMembersContributor extends NonCodeMembersContributor {
  @Override
  public String getParentClassName() {
    return GormUtils.NAMED_CRITERIA_PROXY_CLASS_NAME;
  }

  @Override
  public void processDynamicElements(@NotNull PsiType qualifierType,
                                     @Nullable PsiClass aClass,
                                     @NotNull PsiScopeProcessor processor,
                                     @NotNull PsiElement place,
                                     @NotNull ResolveState state) {

    if (!(place instanceof GrReferenceExpression)) return;

    NamedQueryDescriptor queryDescriptor = GormUtils.getQueryDescriptorByProxyMethod((GrReferenceExpression)place);
    if (queryDescriptor == null) return;

    String nameHint = ResolveUtil.getNameHint(processor);
    ElementClassHint classHint = processor.getHint(ElementClassHint.KEY);

    DomainMembersProvider.processNamedQueries(new DelegatingScopeProcessor(processor) {
      @Override
      public boolean execute(@NotNull PsiElement element, @NotNull ResolveState state) {
        if (GrLightMethodBuilder.checkKind(element, NamedQueryDescriptor.NAMED_QUERY_METHOD_MARKER)) {
          GrLightMethodBuilder copy = (GrLightMethodBuilder)element.copy();
          copy.getModifierList().removeModifier(GrModifierFlags.STATIC_MASK);
          return super.execute(copy, state);
        }

        if (element instanceof GrLightField) {
          if (((GrLightField)element).getCreatorKey() instanceof NamedQueryDescriptor) {
            GrLightField copy = (GrLightField)element.copy();
            copy.getModifierList().removeModifier(GrModifierFlags.STATIC_MASK);
            return super.execute(copy, state);
          }
        }

        return super.execute(element, state);
      }
    }, queryDescriptor.getDomainDescriptor(), nameHint, classHint);
  }
}
