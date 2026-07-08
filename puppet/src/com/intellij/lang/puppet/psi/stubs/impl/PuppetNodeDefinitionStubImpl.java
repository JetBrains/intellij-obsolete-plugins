package com.intellij.lang.puppet.psi.stubs.impl;

import com.intellij.lang.puppet.psi.PuppetNodeDefinition;
import com.intellij.lang.puppet.psi.stubs.PuppetNodeDefinitionStub;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class PuppetNodeDefinitionStubImpl extends PuppetPolyNamedStubElementBase<PuppetNodeDefinition> implements PuppetNodeDefinitionStub {
  public PuppetNodeDefinitionStubImpl(StubElement parent, IStubElementType elementType, @NotNull List<String> namesList) {
    super(parent, elementType, namesList);
  }
}

