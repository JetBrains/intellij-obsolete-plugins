package com.intellij.lang.puppet.psi.stubs.impl;

import com.intellij.lang.puppet.psi.PuppetClassDefinition;
import com.intellij.lang.puppet.psi.stubs.PuppetClassDefinitionStub;
import com.intellij.lang.puppet.psi.stubs.PuppetStubNamedElementBase;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class PuppetClassDefinitionStubImpl extends PuppetStubNamedElementBase<PuppetClassDefinition> implements PuppetClassDefinitionStub {
  private final @Nullable String myParentClassName;

  public PuppetClassDefinitionStubImpl(StubElement parent,
                                       IStubElementType elementType,
                                       @NotNull String name,
                                       @Nullable String namespaceName,
                                       @Nullable String parentClassName) {
    super(parent, elementType, name, namespaceName);

    myParentClassName = parentClassName;
  }

  @Override
  public @Nullable String getParentClassName() {
    return myParentClassName;
  }
}
