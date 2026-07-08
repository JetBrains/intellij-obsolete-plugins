package com.intellij.lang.puppet.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface PuppetPolyNamedPsiElement extends PuppetCompositePsiElement, PuppetPolyNamedElement {
  /**
   * @return Name identifier by name; null if unresovable
   */
  @Nullable
  PsiElement getNameIdentifierByName(@NotNull String name);

  /**
   * @return list of light elements, bound to the names identifiers, one for each name identifier
   */
  @NotNull
  List<PuppetDelegatingLightNamedElement> getLightElementsList();

  /**
   * @return light element bound to the name identifier with name specified; null if unresolvable
   */
  @Nullable
  PuppetDelegatingLightNamedElement getLightElementByName(@NotNull String name);
}
