package com.intellij.lang.puppet.psi.stubs;

import com.intellij.lang.puppet.psi.PuppetResourceInstanceDeclaration;

public interface PuppetResourceInstanceDeclarationStub extends PuppetPolyNamedStubElement<PuppetResourceInstanceDeclaration> {
  String getEffectiveTypeName();

  boolean isExported();

  boolean isVirtual();
}
