package com.intellij.lang.puppet.psi.stubs.elementTypes;

import com.intellij.lang.puppet.psi.impl.PsiPuppetIncludeClassStatementImpl;
import com.intellij.lang.puppet.psi.mixins.PuppetIncludeClassStatementMixin;
import com.intellij.lang.puppet.psi.stubs.impl.PuppetIncludeClassStatementStub;
import org.jetbrains.annotations.NotNull;

public class PuppetIncludeClassStatementElementType extends PuppetIncludeClassStatementElementTypeBase<PuppetIncludeClassStatementMixin> {
  public PuppetIncludeClassStatementElementType(@NotNull String debugName) {
    super(debugName);
  }

  @Override
  public PuppetIncludeClassStatementMixin createPsi(@NotNull PuppetIncludeClassStatementStub stub) {
    return new PsiPuppetIncludeClassStatementImpl(stub, this);
  }
}