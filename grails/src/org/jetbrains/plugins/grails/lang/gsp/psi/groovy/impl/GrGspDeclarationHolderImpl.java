// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.groovy.impl;

import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.GspLazyElement;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GrGspClass;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GrGspDeclarationHolder;
import org.jetbrains.plugins.groovy.lang.psi.GroovyElementVisitor;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariableDeclaration;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.plugins.groovy.lang.psi.util.PsiTreeUtilKt.treeWalkUp;

public class GrGspDeclarationHolderImpl extends GspLazyElement implements GrGspDeclarationHolder {

  public GrGspDeclarationHolderImpl(@NotNull IElementType type, @Nullable CharSequence buffer) {
    super(type, buffer);
  }

  @Override
  public String toString() {
    return "Groovy class level declaration element";
  }

  @Override
  public void accept(@NotNull GroovyElementVisitor visitor) {
  }

  @Override
  public GrField[] getFields() {
    GrVariableDeclaration[] declarations = findChildrenByClass(GrVariableDeclaration.class);
    if (declarations.length == 0) return GrField.EMPTY_ARRAY;
    List<GrField> result = new ArrayList<>();
    for (GrVariableDeclaration declaration : declarations) {
      GrVariable[] variables = declaration.getVariables();
      for (GrVariable variable : variables) {
        if (variable instanceof GrField) {
          result.add((GrField) variable);
        }
      }
    }
    return result.toArray(GrField.EMPTY_ARRAY);
  }


  @Override
  public GrMethod[] getMethods() {
    return findChildrenByClass(GrMethod.class);
  }

  @Override
  public boolean processDeclarations(@NotNull PsiScopeProcessor processor, @NotNull ResolveState state, PsiElement lastParent, @NotNull PsiElement place) {
    GrGspClass clazz = PsiTreeUtil.getParentOfType(this, GrGspClass.class);
    if (clazz != null) {
      if (!clazz.processDeclarations(processor, state, this, place)) return false;
      treeWalkUp(clazz, processor);
    }

    return false; //do not attempt any further resolving
  }
}
