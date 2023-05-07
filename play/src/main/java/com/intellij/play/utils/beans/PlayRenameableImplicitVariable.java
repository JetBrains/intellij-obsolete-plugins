package com.intellij.play.utils.beans;


import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.RenameableFakePsiElement;
import org.jetbrains.annotations.NotNull;

public class PlayRenameableImplicitVariable extends PlayImplicitVariable {

  private final RenameableFakePsiElement myFakePsiElement;

  public PlayRenameableImplicitVariable(@NotNull String name, @NotNull PsiType type, @NotNull RenameableFakePsiElement fakePsiElement) {
    super(name, type, fakePsiElement);
    myFakePsiElement = fakePsiElement;
  }

  @NotNull
  @Override
  public PsiElement getNavigationElement() {
    return myFakePsiElement;
  }
}
