package com.intellij.lang.puppet.psi.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.PuppetTokenTypes;
import com.intellij.lang.puppet.psi.PuppetPsiUtil;
import com.intellij.lang.puppet.psi.impl.PuppetStubBasedQualifiedNamedElement;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PuppetStubBasedNamedPsiElementBase<S extends PuppetStubNamedElement> extends PuppetStubBasedQualifiedNamedElement<S>
  implements PuppetStubBasedNamedPsiElement<S> {
  public PuppetStubBasedNamedPsiElementBase(@NotNull S stub, @NotNull IStubElementType nodeType) {
    super(stub, nodeType);
  }

  public PuppetStubBasedNamedPsiElementBase(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @Nullable PsiElement getNameIdentifier() {
    return findChildByType(PuppetTokenTypes.NAME);
  }

  public @NotNull TextRange getNameRange() {
    PsiElement identifier = getNameIdentifier();
    if (identifier == null) {
      return TextRange.EMPTY_RANGE;
    }

    int offsetInParent = identifier.getStartOffsetInParent();
    return TextRange.create(offsetInParent, offsetInParent + identifier.getTextLength());
  }

  @Override
  public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
    return PuppetPsiUtil.setName(this, name);
  }

  @Override
  public final @Nullable String getName() {
    S stub = getGreenStub();
    if (stub != null) {
      return stub.getName();
    }

    return computeName();
  }

  protected @Nullable String getNameIdentifierText() {
    PsiElement identifier = getNameIdentifier();
    return identifier == null ? null : identifier.getText();
  }

  protected @Nullable String computeName() {
    @NonNls String identifierText = getNameIdentifierText();
    return identifierText == null ? null : StringUtil.toLowerCase(identifierText);
  }

  @Override
  public final @Nullable String getNamespaceName() {
    S stub = getGreenStub();
    if (stub != null) {
      return stub.getNamespaceName();
    }
    return computeNamespaceName();
  }

  protected @Nullable String computeNamespaceName() {
    return null;
  }
}
