package com.intellij.lang.puppet.psi;

import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface PuppetPolyNamedElement {
  /**
   * @return PsiElements holding names of the node; empty list if there is no names list element
   */
  @NotNull
  List<PsiElement> getNameIdentifiersList();

  /**
   * Returns name from identifier
   *
   * @param identifier identifier from getNameIdentifiersList
   * @return name or null if name is unresolvable
   */
  default String getNameFromIdentifier(PsiElement identifier) {
    return ElementManipulators.getValueText(identifier);
  }

  /**
   * @return Names of element
   */
  @NotNull
  List<String> getNamesList();
}
