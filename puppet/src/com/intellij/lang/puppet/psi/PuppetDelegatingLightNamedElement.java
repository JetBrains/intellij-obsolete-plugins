package com.intellij.lang.puppet.psi;

import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PuppetDelegatingLightNamedElement extends PuppetDelegatingLightElement<PuppetPolyNamedPsiElement> implements
                                                                                                            PsiNameIdentifierOwner {
  private final String myName;

  public PuppetDelegatingLightNamedElement(@NotNull PuppetPolyNamedPsiElement delegate, String name) {
    super(delegate);
    myName = name;
  }

  @Override
  public @Nullable PsiElement getNameIdentifier() {
    return getDelegate().getNameIdentifierByName(myName);
  }

  @Override
  public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
    return PuppetPsiUtil.setName(this, name);
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public void navigate(boolean requestFocus) {
    PsiElement nameIdentifier = getNameIdentifier();
    if (nameIdentifier instanceof NavigatablePsiElement) {
      ((NavigatablePsiElement)nameIdentifier).navigate(requestFocus);
    }
    else {
      super.navigate(requestFocus);
    }
  }

  @Override
  public @NotNull PsiElement getNavigationElement() {
    PsiElement nameIdentifier = getNameIdentifier();
    return nameIdentifier == null ? super.getNavigationElement() : nameIdentifier;
  }

  @Override
  public int getTextOffset() {
    PsiElement identifier = getNameIdentifier();
    return identifier == null ? super.getTextOffset() : identifier.getTextOffset();
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PuppetDelegatingLightNamedElement element = (PuppetDelegatingLightNamedElement)o;

    if (!getDelegate().equals(element.getDelegate())) return false;
    if (!myName.equals(element.myName)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return getDelegate().hashCode() * 31 + myName.hashCode();
  }

  @Override
  public boolean isValid() {
    return getDelegate().isValid();
  }
}
