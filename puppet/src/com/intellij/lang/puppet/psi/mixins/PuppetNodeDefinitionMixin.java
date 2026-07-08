package com.intellij.lang.puppet.psi.mixins;

import com.intellij.icons.AllIcons;
import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.psi.PsiPuppetNodeNamesList;
import com.intellij.lang.puppet.psi.PuppetDefaultWrapper;
import com.intellij.lang.puppet.psi.PuppetNodeDefinition;
import com.intellij.lang.puppet.psi.stubs.PuppetNodeDefinitionStub;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.intellij.lang.puppet.PuppetTokenTypes.DEFAULT_WRAPPER;
import static com.intellij.lang.puppet.PuppetTokenTypes.QUOTED_TEXT;
import static com.intellij.lang.puppet.PuppetTokenTypes.REGEXP;
import static com.intellij.lang.puppet.PuppetTokenTypes.REGULAR_NAME_WRAPPER;

public abstract class PuppetNodeDefinitionMixin extends PuppetStubBasedPolyNamedPsiElementBase<PuppetNodeDefinitionStub>
  implements PuppetNodeDefinition {

  private static final TokenSet KNOWN_NAME_HOLDERS = TokenSet.create(
    QUOTED_TEXT,
    DEFAULT_WRAPPER,
    REGULAR_NAME_WRAPPER,
    REGEXP
  );

  public PuppetNodeDefinitionMixin(@NotNull PuppetNodeDefinitionStub stub, @NotNull IStubElementType nodeType) {
    super(stub, nodeType);
  }

  public PuppetNodeDefinitionMixin(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull List<PsiElement> getNameIdentifiersList() {
    PsiPuppetNodeNamesList nodeNamesList = getNodeNamesList();
    return nodeNamesList == null ? Collections.emptyList() : Arrays.asList(nodeNamesList.getChildren());
  }

  @Override
  public String getNameFromIdentifier(PsiElement identifier) {
    if (KNOWN_NAME_HOLDERS.contains(PsiUtilCore.getElementType(identifier))) {
      if (identifier instanceof PuppetDefaultWrapper) {
        return PuppetDefaultWrapper.DEFAULT_NAME;
      }
      return ElementManipulators.getValueText(identifier);
    }
    return null;
  }

  @Override
  public @Nullable Icon getIcon(int flags) {
    return AllIcons.Nodes.WebFolder;
  }

  @Override
  public PsiElement getContext() {
    // fixme we need to be able to walk stubs i believe, but for this we need to implement stubs treeWalkup and ProcessDeclarations
    return getContainingFile();
  }

  @Override
  public boolean processDeclarations(@NotNull PsiScopeProcessor processor,
                                     @NotNull ResolveState resolveState,
                                     @Nullable PsiElement lastChildElement,
                                     @NotNull PsiElement originElement) {
    return super.processDeclarations(processor, resolveState, lastChildElement, originElement);
  }
}
