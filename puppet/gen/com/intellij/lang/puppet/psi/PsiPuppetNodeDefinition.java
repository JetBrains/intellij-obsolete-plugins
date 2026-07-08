// This is a generated file. Not intended for manual editing.
package com.intellij.lang.puppet.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.StubBasedPsiElement;
import com.intellij.lang.puppet.psi.stubs.PuppetNodeDefinitionStub;

public interface PsiPuppetNodeDefinition extends PsiPuppetExpression, PuppetNodeDefinition, StubBasedPsiElement<PuppetNodeDefinitionStub> {

  @Nullable
  PsiPuppetBracedStatementsBlock getBracedStatementsBlock();

  @Nullable
  PsiPuppetNodeNamesList getNodeNamesList();

  @Nullable
  PsiPuppetParentNode getParentNode();

}
