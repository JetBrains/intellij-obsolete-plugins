package com.intellij.lang.puppet.psi;

import org.jetbrains.annotations.Nullable;

import static com.intellij.lang.puppet.util.PuppetQualifiedNamesUtil.MAIN_NAMESPACE;

/**
 * Marks that element contains named scope, available from from the outside
 */
public interface PuppetNamedScopeHolder extends PuppetScopeHolder {
  default @Nullable String getScopeFullQualifiedName() {
    return MAIN_NAMESPACE;
  }

  /**
   * Indicates that scope in this holder is local and variables are not available from the outside
   */
  default boolean isLocalScope() {
    return false;
  }
}
