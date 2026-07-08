package com.intellij.lang.puppet.psi.mixins;

import com.intellij.icons.AllIcons;
import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.psi.PuppetFunctionDefinition;
import com.intellij.lang.puppet.psi.stubs.PuppetFunctionDefinitionStub;
import com.intellij.lang.puppet.psi.stubs.PuppetStubBasedNamedWithFqnContainerPsiElementBase;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public abstract class PuppetFunctionDefinitionMixin
  extends PuppetStubBasedNamedWithFqnContainerPsiElementBase<PuppetFunctionDefinitionStub>
  implements PuppetFunctionDefinition {

  public PuppetFunctionDefinitionMixin(@NotNull PuppetFunctionDefinitionStub stub, @NotNull IStubElementType nodeType) {
    super(stub, nodeType);
  }

  public PuppetFunctionDefinitionMixin(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @Nullable Icon getIcon(int flags) {
    // fixme add an icon
    return AllIcons.Nodes.Field;
  }
}

