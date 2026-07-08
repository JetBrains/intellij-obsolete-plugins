// This is a generated file. Not intended for manual editing.
package com.intellij.lang.puppet.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.StubBasedPsiElement;
import com.intellij.lang.puppet.psi.stubs.PuppetClassDefinitionStub;

public interface PsiPuppetClassDefinition extends PsiPuppetExpression, PuppetClassDefinition, StubBasedPsiElement<PuppetClassDefinitionStub> {

  @Nullable
  PsiPuppetAnyNameWrapper getAnyNameWrapper();

  @Nullable
  PsiPuppetBracedStatementsBlock getBracedStatementsBlock();

  @Nullable
  PsiPuppetFqnContainer getFqnContainer();

  @Nullable
  PsiPuppetParenthesizedParametersListBlock getParenthesizedParametersListBlock();

}
