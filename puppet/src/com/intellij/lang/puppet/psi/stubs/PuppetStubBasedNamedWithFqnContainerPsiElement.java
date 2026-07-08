package com.intellij.lang.puppet.psi.stubs;

import com.intellij.lang.puppet.psi.PuppetFqnContainer;
import org.jetbrains.annotations.Nullable;

public interface PuppetStubBasedNamedWithFqnContainerPsiElement<S extends PuppetStubElement> extends PuppetStubBasedNamedPsiElement<S> {
  @Nullable
  PuppetFqnContainer getFqnContainer();
}
