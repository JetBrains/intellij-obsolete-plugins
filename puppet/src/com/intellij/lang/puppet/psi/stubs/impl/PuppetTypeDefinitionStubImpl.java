package com.intellij.lang.puppet.psi.stubs.impl;

import com.intellij.lang.puppet.psi.PuppetTypeDefinition;
import com.intellij.lang.puppet.psi.stubs.PuppetStubNamedElementBase;
import com.intellij.lang.puppet.psi.stubs.PuppetTypeDefinitionStub;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PuppetTypeDefinitionStubImpl extends PuppetStubNamedElementBase<PuppetTypeDefinition> implements PuppetTypeDefinitionStub {
  public PuppetTypeDefinitionStubImpl(StubElement parent,
                                      IStubElementType elementType,
                                      @NotNull String name,
                                      @Nullable String namespaceName) {
    super(parent, elementType, name, namespaceName);
  }
}
