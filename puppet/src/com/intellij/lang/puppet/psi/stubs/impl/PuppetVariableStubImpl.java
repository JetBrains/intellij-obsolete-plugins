package com.intellij.lang.puppet.psi.stubs.impl;

import com.intellij.lang.puppet.psi.PuppetVariable;
import com.intellij.lang.puppet.psi.stubs.PuppetStubNamedElementBase;
import com.intellij.lang.puppet.psi.stubs.PuppetVariableStub;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PuppetVariableStubImpl extends PuppetStubNamedElementBase<PuppetVariable> implements PuppetVariableStub {
  private final boolean myIsParameter;
  private final boolean myIsDeclaration;

  public PuppetVariableStubImpl(StubElement parent,
                                IStubElementType elementType,
                                @NotNull String name,
                                @Nullable String namespaceName,
                                boolean isDeclaration,
                                boolean isParameter) {
    super(parent, elementType, name, namespaceName);
    myIsDeclaration = isDeclaration;
    myIsParameter = isParameter;
  }

  @Override
  public boolean isParameter() {
    return myIsParameter;
  }

  @Override
  public boolean isDeclaration() {
    return myIsDeclaration;
  }
}
