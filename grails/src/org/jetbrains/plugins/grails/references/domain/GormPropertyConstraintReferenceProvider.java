// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.references.util.PsiFieldReference;
import org.jetbrains.plugins.groovy.lang.completion.CompleteReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrIndexProperty;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrLiteralImpl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GormPropertyConstraintReferenceProvider extends PsiReferenceProvider {
  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    final GrArgumentList argumentList = (GrArgumentList)element.getParent();
    GrIndexProperty index = (GrIndexProperty)argumentList.getParent();

    GrExpression expression = index.getInvokedExpression();
    if (!(expression instanceof GrReferenceExpression ref)) return PsiReference.EMPTY_ARRAY;

    if (!"properties".equals(ref.getReferenceName())) return PsiReference.EMPTY_ARRAY;

    final GrExpression qualifier = ref.getQualifierExpression();
    if (qualifier == null) return PsiReference.EMPTY_ARRAY;

    return new PsiReference[]{
      new PsiFieldReference(element, false) {

        private @Nullable Map<String, Pair<PsiType, PsiElement>> getPropertyMap() {
          PsiClass domainClass = PsiTypesUtil.getPsiClass(qualifier.getType());

          if (!GormUtils.isGormBean(domainClass)) return null;
          assert domainClass != null;

          return DomainDescriptor.getDescriptor(domainClass).getPersistentProperties();
        }

        @Override
        public PsiElement resolve() {
          Map<String, Pair<PsiType, PsiElement>> map = getPropertyMap();
          if (map == null) return null;

          Pair<PsiType, PsiElement> pair = map.get(getValue());
          if (pair == null) return null;

          return pair.second;
        }

        @SuppressWarnings({"unchecked"})
        @Override
        public Object @NotNull [] getVariants() {
          Map<String, Pair<PsiType, PsiElement>> map = getPropertyMap();
          if (map == null) return ArrayUtilRt.EMPTY_OBJECT_ARRAY;

          Set<String> existingFields = new HashSet<>();

          for (GrExpression expression : argumentList.getExpressionArguments()) {
            if (expression instanceof GrLiteralImpl && expression != getElement()) {
              Object value = ((GrLiteralImpl)expression).getValue();
              if (value instanceof String) {
                existingFields.add((String)value);
              }
            }
          }

          List res = new ArrayList();

          for (Map.Entry<String, Pair<PsiType, PsiElement>> entry : map.entrySet()) {
            String name = entry.getKey();

            if (!existingFields.contains(name)) {
              res.add(CompleteReferenceExpression.createPropertyLookupElement(name, entry.getValue().first));
            }
          }

          return res.toArray();
        }
      }
    };
  }
}
