package com.intellij.lang.puppet.psi.stubs.elementTypes;

import com.intellij.lang.puppet.psi.PuppetFunctionDefinition;
import com.intellij.lang.puppet.psi.impl.PsiPuppetFunctionDefinitionImpl;
import com.intellij.lang.puppet.psi.stubs.PuppetFunctionDefinitionStub;
import com.intellij.lang.puppet.psi.stubs.impl.PuppetFunctionDefinitionStubImpl;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetFunctionsStubsIndex;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubIndexKey;
import com.intellij.psi.stubs.StubInputStream;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class PuppetFunctionDefinitionElementType
  extends PuppetStubNamedElementTypeBase<PuppetFunctionDefinitionStub, PuppetFunctionDefinition> {
  public PuppetFunctionDefinitionElementType(@NotNull @NonNls String debugName) {
    super(debugName);
  }

  @Override
  public PuppetFunctionDefinition createPsi(@NotNull PuppetFunctionDefinitionStub stub) {
    return new PsiPuppetFunctionDefinitionImpl(stub, this);
  }

  @Override
  protected StubIndexKey<String, PuppetFunctionDefinition> getIndexKey() {
    return PuppetFunctionsStubsIndex.KEY;
  }

  @Override
  public @NotNull PuppetFunctionDefinitionStub createStub(@NotNull PuppetFunctionDefinition psi, StubElement parentStub) {
    String name = psi.getName();
    assert name != null : "shouldCreateStub should filter out nameless entities";
    return new PuppetFunctionDefinitionStubImpl(parentStub, this, name, psi.getNamespaceName());
  }


  @Override
  protected PuppetFunctionDefinitionStub deserializeRest(@NotNull StubInputStream dataStream,
                                                         StubElement<?> parentStub,
                                                         @NotNull String name,
                                                         @Nullable String namespaceName) throws IOException {
    return new PuppetFunctionDefinitionStubImpl(parentStub, this, name, namespaceName);
  }

}
