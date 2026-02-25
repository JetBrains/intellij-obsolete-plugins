// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.pluginSupport.seachable;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.domain.GormPropertyReferenceUnique;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrAssignmentExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrLiteralImpl;

public class SearchableFieldReferenceProvider extends PsiReferenceProvider {

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    PsiElement parent = element.getParent();

    if (parent instanceof GrListOrMap) parent = parent.getParent();

    PsiElement eField;
    String labelName;

    if (parent instanceof GrNamedArgument) {
      labelName = ((GrNamedArgument)parent).getLabelName();

      PsiElement listOrMap = parent.getParent();
      if (!(listOrMap instanceof GrListOrMap)) return PsiReference.EMPTY_ARRAY;

      eField = listOrMap.getParent();
    }
    else if (parent instanceof GrAssignmentExpression assExpr) {

      GrExpression lValue = assExpr.getLValue();
      if (!(lValue instanceof GrReferenceExpression) || ((GrReferenceExpression)lValue).isQualified()) return PsiReference.EMPTY_ARRAY;
      labelName = ((GrReferenceExpression)lValue).getReferenceName();

      PsiElement searchableClosure = assExpr.getParent();
      if (!(searchableClosure instanceof GrClosableBlock)) return PsiReference.EMPTY_ARRAY;

      eField = searchableClosure.getParent();
    }
    else {
      return PsiReference.EMPTY_ARRAY;
    }

    if (!(eField instanceof GrField field)) return PsiReference.EMPTY_ARRAY;

    if (!GrailsSearchableUtil.isSearchableField(field)) return PsiReference.EMPTY_ARRAY;
    if (!"except".equals(labelName) && !"only".equals(labelName)) return PsiReference.EMPTY_ARRAY;

    GrLiteralImpl literal = (GrLiteralImpl)element;
    String value = (String)literal.getValue();
    assert value != null;

    if (value.indexOf('?') != -1 || value.indexOf('*') != -1) return PsiReference.EMPTY_ARRAY;

    return new PsiReference[]{new GormPropertyReferenceUnique(element, false, field.getContainingClass())};
  }
}
