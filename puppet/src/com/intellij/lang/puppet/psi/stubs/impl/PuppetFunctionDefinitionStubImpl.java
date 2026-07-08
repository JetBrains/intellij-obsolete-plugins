package com.intellij.lang.puppet.psi.stubs.impl;

import com.intellij.lang.puppet.psi.PuppetFunctionDefinition;
import com.intellij.lang.puppet.psi.stubs.PuppetFunctionDefinitionStub;
import com.intellij.lang.puppet.psi.stubs.PuppetStubNamedElementBase;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PuppetFunctionDefinitionStubImpl extends PuppetStubNamedElementBase<PuppetFunctionDefinition>
  implements PuppetFunctionDefinitionStub {

  public PuppetFunctionDefinitionStubImpl(StubElement parent,
                                          IStubElementType elementType,
                                          @NotNull String name,
                                          @Nullable String namespaceName) {
    super(parent, elementType, name, namespaceName);
  }
}
