package com.intellij.lang.puppet.psi.mixins;

import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.psi.PuppetFqnContainer;
import com.intellij.lang.puppet.psi.PuppetNamespaceDefinition;
import com.intellij.lang.puppet.psi.stubs.PuppetNamespaceDefinitionStub;
import com.intellij.lang.puppet.psi.stubs.PuppetStubBasedNamedPsiElementBase;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PuppetNamespaceDefinitionMixin extends PuppetStubBasedNamedPsiElementBase<PuppetNamespaceDefinitionStub>
  implements PuppetNamespaceDefinition {

  public PuppetNamespaceDefinitionMixin(@NotNull PuppetNamespaceDefinitionStub stub,
                                        @NotNull IStubElementType nodeType) {
    super(stub, nodeType);
  }

  public PuppetNamespaceDefinitionMixin(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @Nullable String getContainingNamespaceName() {
    PsiElement parent = getParent();
    assert parent instanceof PuppetFqnContainer : "Got " + parent + " instead of PuppetFqnContainer";

    return ((PuppetFqnContainer)parent).getContainingNamespaceNameForElement(this);
  }

  @Override
  protected @Nullable String computeNamespaceName() {
    return getContainingNamespaceName();
  }

  @Override
  public PsiElement setName(@NotNull String name) throws IncorrectOperationException {
    return super.setName(StringUtil.toLowerCase(name));
  }
}
