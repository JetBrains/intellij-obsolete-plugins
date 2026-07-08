package com.intellij.lang.puppet.psi.stubs.elementTypes;

import com.intellij.lang.puppet.psi.PuppetClassDefinition;
import com.intellij.lang.puppet.psi.impl.PsiPuppetClassDefinitionImpl;
import com.intellij.lang.puppet.psi.stubs.PuppetClassDefinitionStub;
import com.intellij.lang.puppet.psi.stubs.impl.PuppetClassDefinitionStubImpl;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetClassStubsIndex;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetSubClassStubsIndex;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubIndexKey;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;


public class PuppetClassDefinitionElementType extends PuppetStubNamedElementTypeBase<PuppetClassDefinitionStub, PuppetClassDefinition> {
  public PuppetClassDefinitionElementType(@NotNull @NonNls String debugName) {
    super(debugName);
  }

  @Override
  public PuppetClassDefinition createPsi(@NotNull PuppetClassDefinitionStub stub) {
    return new PsiPuppetClassDefinitionImpl(stub, this);
  }

  @Override
  public @NotNull PuppetClassDefinitionStub createStub(@NotNull PuppetClassDefinition psi, StubElement parentStub) {
    String name = psi.getName();
    assert name != null : "shouldCreateStub should filter out nameless entities";
    return new PuppetClassDefinitionStubImpl(parentStub, this, name, psi.getNamespaceName(), psi.getParentClassName());
  }

  @Override
  protected void serializeRest(@NotNull PuppetClassDefinitionStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getParentClassName());
  }

  @Override
  protected PuppetClassDefinitionStub deserializeRest(@NotNull StubInputStream dataStream,
                                                      StubElement<?> parentStub,
                                                      @NotNull String name,
                                                      @Nullable String namespaceName) throws IOException {
    return new PuppetClassDefinitionStubImpl(parentStub, this, name, namespaceName, dataStream.readNameString());
  }


  @Override
  protected StubIndexKey<String, PuppetClassDefinition> getIndexKey() {
    return PuppetClassStubsIndex.KEY;
  }

  @Override
  public void indexStub(@NotNull PuppetClassDefinitionStub stub, @NotNull IndexSink sink) {
    super.indexStub(stub, sink);
    String parentClassName = stub.getParentClassName();
    if (StringUtil.isNotEmpty(parentClassName)) {
      sink.occurrence(PuppetSubClassStubsIndex.KEY, parentClassName);
    }
  }
}
