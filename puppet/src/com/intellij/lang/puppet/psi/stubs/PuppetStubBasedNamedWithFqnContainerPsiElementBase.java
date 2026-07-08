package com.intellij.lang.puppet.psi.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.psi.PuppetFqnContainer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PuppetStubBasedNamedWithFqnContainerPsiElementBase<S extends PuppetStubNamedElement>
  extends PuppetStubBasedNamedPsiElementBase<S>
  implements PuppetStubBasedNamedWithFqnContainerPsiElement<S> {
  public PuppetStubBasedNamedWithFqnContainerPsiElementBase(@NotNull S stub, @NotNull IStubElementType nodeType) {
    super(stub, nodeType);
  }

  public PuppetStubBasedNamedWithFqnContainerPsiElementBase(@NotNull ASTNode node) {
    super(node);
  }


  @Override
  public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
    return super.setName(StringUtil.toLowerCase(name));
  }

  @Override
  protected @Nullable String computeNamespaceName() {
    PuppetFqnContainer container = getFqnContainer();
    return container == null ? null : container.getNamespaceName();
  }

  @Override
  public @Nullable PsiElement getNameIdentifier() {
    PuppetFqnContainer fqnContainer = getFqnContainer();
    return fqnContainer == null ? null : fqnContainer.getNameIdentifier();
  }
}
