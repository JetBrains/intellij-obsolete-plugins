package com.intellij.lang.puppet.psi;

import com.intellij.lang.puppet.psi.stubs.PuppetStubBasedNamedWithFqnContainerPsiElement;
import com.intellij.lang.puppet.psi.stubs.PuppetTypeDefinitionStub;

public interface PuppetTypeDefinition extends PuppetStubBasedNamedWithFqnContainerPsiElement<PuppetTypeDefinitionStub>,
                                              PuppetScopeHolder,
                                              PuppetParametrizedDeclaration {

  boolean isMetaparameterContainingType();
}
