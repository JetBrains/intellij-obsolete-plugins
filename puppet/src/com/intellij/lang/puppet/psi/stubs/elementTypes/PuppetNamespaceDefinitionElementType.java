package com.intellij.lang.puppet.psi.stubs.elementTypes;

import com.intellij.lang.puppet.psi.PuppetNamespaceDefinition;
import com.intellij.lang.puppet.psi.impl.PsiPuppetNamespaceDefinitionImpl;
import com.intellij.lang.puppet.psi.stubs.PuppetNamespaceDefinitionStub;
import com.intellij.lang.puppet.psi.stubs.impl.PuppetNamespaceDefinitionStubImpl;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetNamespacesStubsIndex;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubIndexKey;
import com.intellij.psi.stubs.StubInputStream;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class PuppetNamespaceDefinitionElementType
  extends PuppetStubNamedElementTypeBase<PuppetNamespaceDefinitionStub, PuppetNamespaceDefinition> {
  public PuppetNamespaceDefinitionElementType(@NotNull @NonNls String debugName) {
    super(debugName);
  }


  @Override
  public PuppetNamespaceDefinition createPsi(@NotNull PuppetNamespaceDefinitionStub stub) {
    return new PsiPuppetNamespaceDefinitionImpl(stub, this);
  }

  @Override
  public @NotNull PuppetNamespaceDefinitionStub createStub(@NotNull PuppetNamespaceDefinition psi, StubElement parentStub) {
    String name = psi.getName();
    assert name != null : "shouldCreateStub should filter out nameless entities";
    return new PuppetNamespaceDefinitionStubImpl(parentStub, this, name, psi.getNamespaceName());
  }

  @Override
  protected StubIndexKey<String, PuppetNamespaceDefinition> getIndexKey() {
    return PuppetNamespacesStubsIndex.KEY;
  }

  @Override
  protected PuppetNamespaceDefinitionStub deserializeRest(@NotNull StubInputStream dataStream,
                                                          StubElement<?> parentStub,
                                                          @NotNull String name,
                                                          @Nullable String namespaceName) throws IOException {
    return new PuppetNamespaceDefinitionStubImpl(parentStub, this, name, namespaceName);
  }
}
