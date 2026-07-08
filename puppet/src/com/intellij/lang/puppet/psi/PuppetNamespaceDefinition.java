package com.intellij.lang.puppet.psi;

import com.intellij.lang.puppet.psi.stubs.PuppetNamespaceDefinitionStub;
import com.intellij.lang.puppet.psi.stubs.PuppetStubBasedNamedPsiElement;
import org.jetbrains.annotations.Nullable;

public interface PuppetNamespaceDefinition extends PuppetStubBasedNamedPsiElement<PuppetNamespaceDefinitionStub> {

  @Nullable
  String getContainingNamespaceName();

  void subtreeChanged();
}
