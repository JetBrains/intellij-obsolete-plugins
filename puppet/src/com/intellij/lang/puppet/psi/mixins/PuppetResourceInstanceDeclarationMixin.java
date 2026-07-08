package com.intellij.lang.puppet.psi.mixins;

import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.PuppetIcons;
import com.intellij.lang.puppet.psi.PuppetDelegatingLightNamedElement;
import com.intellij.lang.puppet.psi.PuppetPsiUtil;
import com.intellij.lang.puppet.psi.PuppetResourceDeclaration;
import com.intellij.lang.puppet.psi.PuppetResourceInstanceDeclaration;
import com.intellij.lang.puppet.psi.PuppetResourceInstanceDelegatingLightNamedElement;
import com.intellij.lang.puppet.psi.stubs.PuppetResourceInstanceDeclarationStub;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.List;

public abstract class PuppetResourceInstanceDeclarationMixin
  extends PuppetStubBasedPolyNamedPsiElementBase<PuppetResourceInstanceDeclarationStub>
  implements PuppetResourceInstanceDeclaration {

  public PuppetResourceInstanceDeclarationMixin(@NotNull PuppetResourceInstanceDeclarationStub stub,
                                                @NotNull IStubElementType nodeType) {
    super(stub, nodeType);
  }

  public PuppetResourceInstanceDeclarationMixin(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull List<PsiElement> getNameIdentifiersList() {
    return PuppetPsiUtil.getResourceLikeIdentifiersList(getFirstChild());
  }

  @Override
  public String getNameFromIdentifier(PsiElement nameIdentifier) {
    return PuppetPsiUtil.getResourceLikeNameFromIdentifier(nameIdentifier);
  }

  @Override
  protected @NotNull PuppetDelegatingLightNamedElement createDelegatingElement(@NotNull String name) {
    return new PuppetResourceInstanceDelegatingLightNamedElement(this, name);
  }

  @Override
  public @Nullable String getEffectiveTypeName() {
    PuppetResourceInstanceDeclarationStub stub = getGreenStub();
    if (stub != null) {
      return stub.getEffectiveTypeName();
    }

    PuppetResourceDeclaration resourceDeclaration = getContainingResource();
    return resourceDeclaration == null ? null : resourceDeclaration.getEffectiveTypeName();
  }

  private @Nullable PuppetResourceDeclaration getContainingResource() {
    return PsiTreeUtil.getParentOfType(this, PuppetResourceDeclaration.class);
  }

  @Override
  public @Nullable Icon getIcon(int flags) {
    return PuppetIcons.ResourceInstance;
  }

  @Override
  public boolean isExported() {
    PuppetResourceInstanceDeclarationStub stub = getGreenStub();
    if (stub != null) {
      return stub.isExported();
    }

    PuppetResourceDeclaration resource = getContainingResource();
    return resource != null && resource.isExported();
  }

  @Override
  public boolean isVirtual() {
    PuppetResourceInstanceDeclarationStub stub = getGreenStub();
    if (stub != null) {
      return stub.isVirtual();
    }

    PuppetResourceDeclaration resource = getContainingResource();
    return resource != null && resource.isVirtual();
  }
}
