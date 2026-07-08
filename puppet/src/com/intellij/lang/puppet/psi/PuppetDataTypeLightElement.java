package com.intellij.lang.puppet.psi;

import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.light.LightElement;
import org.jetbrains.annotations.NotNull;

public class PuppetDataTypeLightElement extends LightElement {

  private final @NotNull String myName;

  public PuppetDataTypeLightElement(@NotNull PsiManager manager, @NotNull String name) {
    super(manager, PuppetLanguage.INSTANCE);
    myName = name;
  }

  @Override
  public @NotNull String getName() {
    return myName;
  }

  @Override
  public String toString() {
    return "PuppetDataTypeLightElement: " + myName;
  }
}
