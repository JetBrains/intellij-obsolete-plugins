package com.intellij.lang.puppet.psi.mixins;

import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.PuppetIcons;
import com.intellij.lang.puppet.ide.libraries.PuppetLibraryUtil;
import com.intellij.lang.puppet.psi.PuppetDelegatingLightElement;
import com.intellij.lang.puppet.psi.PuppetResourceInstanceDeclaration;
import com.intellij.lang.puppet.psi.PuppetTypeDefinition;
import com.intellij.lang.puppet.psi.PupppetTopScopeHolder;
import com.intellij.lang.puppet.psi.resolve.PuppetResolveUtil;
import com.intellij.lang.puppet.psi.stubs.PuppetStubBasedClassContainedPsiElementBase;
import com.intellij.lang.puppet.psi.stubs.PuppetTypeDefinitionStub;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetResourceInstanceByTypeStubsIndex;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetTypeStubIndex;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public abstract class PuppetTypeDefinitionMixin extends PuppetStubBasedClassContainedPsiElementBase<PuppetTypeDefinitionStub>
  implements PuppetTypeDefinition {
  public PuppetTypeDefinitionMixin(@NotNull PuppetTypeDefinitionStub stub, @NotNull IStubElementType nodeType) {
    super(stub, nodeType);
  }

  public PuppetTypeDefinitionMixin(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @Nullable Icon getIcon(int flags) {
    return PuppetIcons.ResourceType;
  }

  @Override
  public PsiElement getContext() {
    return isMetaparameterContainingType() ? null : new ResolveContext(this);
  }

  @Override
  public boolean isMetaparameterContainingType() {
    return StringUtil.equals(getFullQualifiedName(), PuppetLibraryUtil.PUPPET_METAPARAMETERS_STUB_TYPE_NAME);
  }

  private static class ResolveContext extends PuppetDelegatingLightElement<PuppetTypeDefinition> {
    ResolveContext(@NotNull PuppetTypeDefinition delegate) {
      super(delegate);
    }

    @Override
    public PsiElement getContext() {
      return null;
    }

    @Override
    public boolean processDeclarations(@NotNull PsiScopeProcessor processor,
                                       @NotNull ResolveState resloveState,
                                       @Nullable PsiElement lastChild,
                                       @NotNull PsiElement originElement) {
      String typeName = getName();
      if (StringUtil.isEmpty(typeName)) {
        return true;
      }

      if (!getDelegate().isMetaparameterContainingType()) {
        for (PuppetTypeDefinition typeDefinition : PuppetTypeStubIndex.getMetaparametersContainingTypes(this)) {
          if (!typeDefinition.processDeclarations(processor, resloveState, null, originElement)) {
            return false;
          }
        }
      }

      for (PuppetResourceInstanceDeclaration declaration : PuppetResourceInstanceByTypeStubsIndex.getInstance().find(typeName, this)) {
        // fixme this is a weak algorythm. We need to find a closest scope, check what it is and traverse differently. e.g. include->class->class->type should traverse include points
        PupppetTopScopeHolder closestTopScopeHolder = PsiTreeUtil.getStubOrPsiParentOfType(declaration, PupppetTopScopeHolder.class);
        if (closestTopScopeHolder != null) {
          PuppetResolveUtil.treeWalkUp(closestTopScopeHolder, true, processor, resloveState);
        }
      }

      return true;
    }
  }
}
