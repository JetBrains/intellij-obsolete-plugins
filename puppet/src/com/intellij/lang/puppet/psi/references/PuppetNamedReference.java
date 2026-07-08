package com.intellij.lang.puppet.psi.references;

import org.jetbrains.annotations.NotNull;

public interface PuppetNamedReference {
  /**
   * @return presentable name to show in Can't resolve {0} message
   */
  @NotNull
  String getPresentableName();
}
