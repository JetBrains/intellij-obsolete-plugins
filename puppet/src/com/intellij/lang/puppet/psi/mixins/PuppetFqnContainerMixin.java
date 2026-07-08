package com.intellij.lang.puppet.psi.mixins;

import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.PuppetTokenTypes;
import com.intellij.lang.puppet.psi.PsiPuppetNamespaceDefinition;
import com.intellij.lang.puppet.psi.PuppetFqnContainer;
import com.intellij.lang.puppet.psi.impl.PuppetCompositePsiElementBase;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PuppetFqnContainerMixin extends PuppetCompositePsiElementBase implements PuppetFqnContainer {
  public PuppetFqnContainerMixin(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull PsiElement getNameIdentifier() {
    return findNotNullChildByType(PuppetTokenTypes.NAME);
  }

  @Override
  public @Nullable String getContainingNamespaceNameForElement(@NotNull PsiElement element) {
    assert this.equals(element.getParent()) : "This works only with direct children";

    PsiElement run = getFirstChild();
    if (element.equals(run)) {
      return null;
    }

    StringBuilder nameBuilder = new StringBuilder();
    element = element.getPrevSibling(); // preceeding ::
    while (!run.equals(element)) {
      nameBuilder.append(run.getText());
      run = run.getNextSibling();
    }

    return StringUtil.toLowerCase(nameBuilder.toString());
  }

  @Override
  public @Nullable String getNamespaceName() {
    return getContainingNamespaceNameForElement(getNameIdentifier());
  }

  @Override
  public @Nullable String getFullQualifiedName() {
    return StringUtil.toLowerCase(getText());
  }

  @Override
  public void subtreeChanged() {
    super.subtreeChanged();
    for (PsiPuppetNamespaceDefinition definition : getNamespaceDefinitionList()) {
      definition.subtreeChanged();
    }
  }
}
