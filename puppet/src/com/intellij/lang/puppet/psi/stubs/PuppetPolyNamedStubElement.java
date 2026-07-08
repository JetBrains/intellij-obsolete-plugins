package com.intellij.lang.puppet.psi.stubs;

import com.intellij.lang.puppet.psi.PuppetPolyNamedPsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface PuppetPolyNamedStubElement<T extends PuppetPolyNamedPsiElement> extends PuppetStubElement<T> {
  /**
   * @return Names of node definition; empty list if there are no names
   */
  @NotNull
  List<String> getNamesList();
}
