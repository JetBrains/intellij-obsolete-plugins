package com.intellij.lang.puppet.psi;

import com.intellij.lang.puppet.psi.stubs.PuppetFunctionDefinitionStub;
import com.intellij.lang.puppet.psi.stubs.PuppetStubBasedNamedWithFqnContainerPsiElement;

public interface PuppetFunctionDefinition extends PuppetStubBasedNamedWithFqnContainerPsiElement<PuppetFunctionDefinitionStub>,
                                                  PuppetScopeHolder,
                                                  PuppetParametrizedDeclaration {
}
