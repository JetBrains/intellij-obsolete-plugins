package com.intellij.lang.puppet.psi;

import com.intellij.lang.puppet.psi.stubs.PuppetResourceInstanceDeclarationStub;

public interface PuppetResourceInstanceDeclaration extends PuppetStubBasedPolyNamedPsiElement<PuppetResourceInstanceDeclarationStub>,
                                                           PuppetPolyNamedPsiElement,
                                                           PuppetTypeHolder {
  String SEPARATOR = "@";
  String HEAVY_NAME = "resource with heavy name";
  String HEAVY_PRESENTABLE_NAME = "???";

  boolean isExported();

  boolean isVirtual();
}
