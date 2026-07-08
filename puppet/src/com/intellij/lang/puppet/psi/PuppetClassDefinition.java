package com.intellij.lang.puppet.psi;

import com.intellij.lang.puppet.psi.stubs.PuppetClassDefinitionStub;
import com.intellij.lang.puppet.psi.stubs.PuppetStubBasedNamedWithFqnContainerPsiElement;
import com.intellij.psi.search.PsiElementProcessor;
import org.jetbrains.annotations.Nullable;

public interface PuppetClassDefinition extends PuppetStubBasedNamedWithFqnContainerPsiElement<PuppetClassDefinitionStub>,
                                               PuppetNamedScopeHolder,
                                               PuppetParametrizedDeclaration {
  @Nullable
  String getParentClassName();

  @Nullable
  PuppetClassDefinition getParentClass();

  @Nullable
  PuppetNameWrapper getAnyNameWrapper();

  boolean processSubClasses(PsiElementProcessor<? super PuppetClassDefinition> processor);

  boolean processParentClasses(PsiElementProcessor<? super PuppetClassDefinition> processor);
}
