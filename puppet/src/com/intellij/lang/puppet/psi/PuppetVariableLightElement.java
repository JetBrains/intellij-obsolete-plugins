package com.intellij.lang.puppet.psi;

import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.light.LightElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PuppetVariableLightElement extends LightElement implements PuppetVariable {
  private final @Nullable String myNamespaceName;

  private final @NotNull String myName;

  public PuppetVariableLightElement(@NotNull PsiManager manager, @Nullable String namespaceName, @NotNull String name) {
    super(manager, PuppetLanguage.INSTANCE);
    myNamespaceName = namespaceName;
    myName = name;
  }

  public @NotNull String getLookupTypeText() {
    return PuppetBundle.message("puppet.lookup.type.builtin.variable");
  }

  public boolean getLookupBoldness() {
    return true;
  }

  @Override
  public @Nullable String getNamespaceName() {
    return myNamespaceName;
  }

  @Override
  public @NotNull String getName() {
    return myName;
  }

  @Override
  public boolean isDeclaration() {
    return true;
  }

  @Override
  public boolean isMetaparameter() {
    return false;
  }

  @Override
  public boolean isParameter() {
    return false;
  }

  @Override
  public boolean isCoreFact() {
    return false;
  }

  @Override
  public boolean isBuiltIn() {
    return false;
  }

  @Override
  public boolean isFullQualified() {
    return false;
  }

  @Override
  public @Nullable PsiElement getNameIdentifier() {
    return null;
  }

  @Override
  public boolean isLexicalDeclaration() {
    return false;
  }

  @Override
  public @Nullable PuppetScopeHolder getScopeHolder() {
    return null;
  }

  @Override
  public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
    throw new IncorrectOperationException("Not supported");
  }

  @Override
  public String toString() {
    return "PuppetVariableLightElement: " + getFullQualifiedName();
  }
}
