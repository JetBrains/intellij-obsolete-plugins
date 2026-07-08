package com.intellij.lang.puppet.psi.mixins;

import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.psi.PsiPuppetExpression;
import com.intellij.lang.puppet.psi.PsiPuppetParameter;
import com.intellij.lang.puppet.psi.PuppetVariable;
import com.intellij.lang.puppet.psi.impl.PuppetCompositePsiElementBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class PuppetParameterMixin extends PuppetCompositePsiElementBase implements PsiPuppetParameter {
  public PuppetParameterMixin(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * @return Parameter's variable
   */
  public @Nullable PuppetVariable getVariable() {
    List<PsiPuppetExpression> list = getExpressionList();
    if (list.isEmpty()) {
      return null;
    }
    PsiPuppetExpression expression = list.get(0);
    if (expression instanceof PuppetVariable) {
      return (PuppetVariable)expression;
    }

    if (list.size() < 2) {
      return null;
    }
    expression = list.get(1);
    if (expression instanceof PuppetVariable) {
      return (PuppetVariable)expression;
    }

    return null;
  }
}
