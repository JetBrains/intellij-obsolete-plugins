package com.intellij.lang.puppet.psi.stubs;

import com.intellij.lang.puppet.psi.PuppetVariable;

public interface PuppetVariableStub extends PuppetStubNamedElement<PuppetVariable> {
  boolean isDeclaration();

  boolean isParameter();
}
