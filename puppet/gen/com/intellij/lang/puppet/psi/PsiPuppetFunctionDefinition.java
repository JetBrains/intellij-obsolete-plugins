// This is a generated file. Not intended for manual editing.
package com.intellij.lang.puppet.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.StubBasedPsiElement;
import com.intellij.lang.puppet.psi.stubs.PuppetFunctionDefinitionStub;

public interface PsiPuppetFunctionDefinition extends PsiPuppetExpression, PuppetFunctionDefinition, StubBasedPsiElement<PuppetFunctionDefinitionStub> {

  @NotNull
  List<PsiPuppetBlock> getBlockList();

  @Nullable
  PsiPuppetFqnContainer getFqnContainer();

  @Nullable
  PsiPuppetFunctionReturnType getFunctionReturnType();

}
