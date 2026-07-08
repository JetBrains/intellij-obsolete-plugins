package com.intellij.lang.puppet.psi.stubs.impl;

import com.intellij.lang.puppet.psi.PuppetNamespaceDefinition;
import com.intellij.lang.puppet.psi.stubs.PuppetNamespaceDefinitionStub;
import com.intellij.lang.puppet.psi.stubs.PuppetStubNamedElementBase;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PuppetNamespaceDefinitionStubImpl extends PuppetStubNamedElementBase<PuppetNamespaceDefinition>
  implements PuppetNamespaceDefinitionStub {

  public PuppetNamespaceDefinitionStubImpl(StubElement parent,
                                           IStubElementType elementType,
                                           @NotNull String name,
                                           @Nullable String namespaceName) {
    super(parent, elementType, name, namespaceName);
  }
}
