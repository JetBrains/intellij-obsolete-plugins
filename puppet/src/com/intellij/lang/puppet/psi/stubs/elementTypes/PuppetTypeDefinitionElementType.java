package com.intellij.lang.puppet.psi.stubs.elementTypes;

import com.intellij.lang.puppet.psi.PuppetTypeDefinition;
import com.intellij.lang.puppet.psi.impl.PsiPuppetTypeDefinitionImpl;
import com.intellij.lang.puppet.psi.stubs.PuppetTypeDefinitionStub;
import com.intellij.lang.puppet.psi.stubs.impl.PuppetTypeDefinitionStubImpl;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetTypeStubIndex;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubIndexKey;
import com.intellij.psi.stubs.StubInputStream;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class PuppetTypeDefinitionElementType extends PuppetStubNamedElementTypeBase<PuppetTypeDefinitionStub, PuppetTypeDefinition> {
  public PuppetTypeDefinitionElementType(@NotNull @NonNls String debugName) {
    super(debugName);
  }

  @Override
  protected PuppetTypeDefinitionStub deserializeRest(@NotNull StubInputStream dataStream,
                                                     StubElement<?> parentStub,
                                                     @NotNull String name,
                                                     @Nullable String namespaceName) throws IOException {
    return new PuppetTypeDefinitionStubImpl(parentStub, this, name, namespaceName);
  }

  @Override
  protected StubIndexKey<String, PuppetTypeDefinition> getIndexKey() {
    return PuppetTypeStubIndex.KEY;
  }

  @Override
  public PuppetTypeDefinition createPsi(@NotNull PuppetTypeDefinitionStub stub) {
    return new PsiPuppetTypeDefinitionImpl(stub, this);
  }

  @Override
  public @NotNull PuppetTypeDefinitionStub createStub(@NotNull PuppetTypeDefinition psi, StubElement parentStub) {
    String name = psi.getName();
    assert name != null : "shouldCreateStub method should filter this";
    return new PuppetTypeDefinitionStubImpl(parentStub, this, name, psi.getNamespaceName());
  }
}
