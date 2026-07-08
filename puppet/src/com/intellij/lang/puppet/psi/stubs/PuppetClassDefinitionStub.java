package com.intellij.lang.puppet.psi.stubs;

import com.intellij.lang.puppet.psi.PuppetClassDefinition;

public interface PuppetClassDefinitionStub extends PuppetStubNamedElement<PuppetClassDefinition> {
  String getParentClassName();
}

