package com.intellij.lang.puppet.psi.stubs.elementTypes;

import com.intellij.lang.puppet.psi.impl.PsiPuppetIncludeClassExpressionImpl;
import com.intellij.lang.puppet.psi.mixins.PuppetIncludeClassExpressionMixin;
import com.intellij.lang.puppet.psi.stubs.impl.PuppetIncludeClassStatementStub;
import org.jetbrains.annotations.NotNull;

public class PuppetIncludeClassExpressionElementType extends PuppetIncludeClassStatementElementTypeBase<PuppetIncludeClassExpressionMixin> {

  public PuppetIncludeClassExpressionElementType(@NotNull String debugName) {
    super(debugName);
  }

  @Override
  public PuppetIncludeClassExpressionMixin createPsi(@NotNull PuppetIncludeClassStatementStub stub) {
    return new PsiPuppetIncludeClassExpressionImpl(stub, this);
  }
}