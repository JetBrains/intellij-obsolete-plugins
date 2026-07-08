package com.intellij.lang.puppet.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

/**
 * Classes implementing this interface contains a single type information
 * fixme actually it contains a name and used as type and name; guess we should split this interface into two
 */
public interface PuppetTypeHolder extends PsiElement {
  /**
   * This method should not use heavy operations, like variables resolving. Only PSI traversing
   * @return Returns type fqn this element resolves to; Null if it's not possible to compute
   */
  @Nullable
  String getEffectiveTypeName();
}
