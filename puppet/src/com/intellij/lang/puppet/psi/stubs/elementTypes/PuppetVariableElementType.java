package com.intellij.lang.puppet.psi.stubs.elementTypes;

import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.psi.PuppetVariable;
import com.intellij.lang.puppet.psi.impl.PsiPuppetVarWrapperImpl;
import com.intellij.lang.puppet.psi.stubs.PuppetVariableStub;
import com.intellij.lang.puppet.psi.stubs.impl.PuppetVariableStubImpl;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetTopLevelVariablesStubsIndex;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetVariableStubsIndex;
import com.intellij.lang.puppet.util.PuppetQualifiedNamesUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubIndexKey;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;


public class PuppetVariableElementType extends PuppetStubNamedElementTypeBase<PuppetVariableStub, PuppetVariable> {
  public PuppetVariableElementType(@NotNull @NonNls String debugName) {
    super(debugName);
  }

  @Override
  public PuppetVariable createPsi(@NotNull PuppetVariableStub stub) {
    return new PsiPuppetVarWrapperImpl(stub, this);
  }

  @Override
  public @NotNull PuppetVariableStub createStub(@NotNull PuppetVariable psi, StubElement parentStub) {
    String name = psi.getName();
    assert name != null : "shouldCreateStub method should filter this";
    return new PuppetVariableStubImpl(parentStub, this, name, psi.getNamespaceName(), psi.isDeclaration(), psi.isParameter());
  }

  @Override
  protected void serializeRest(@NotNull PuppetVariableStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeBoolean(stub.isDeclaration());
    dataStream.writeBoolean(stub.isParameter());
  }

  @Override
  protected PuppetVariableStub deserializeRest(@NotNull StubInputStream dataStream,
                                               StubElement<?> parentStub,
                                               @NotNull String name,
                                               @Nullable String namespaceName) throws IOException {
    return new PuppetVariableStubImpl(
      parentStub,
      this,
      name,
      namespaceName,
      dataStream.readBoolean(),
      dataStream.readBoolean()
    );
  }

  @Override
  protected StubIndexKey<String, PuppetVariable> getIndexKey() {
    return PuppetVariableStubsIndex.KEY;
  }

  @Override
  public void indexStub(@NotNull PuppetVariableStub stub, @NotNull IndexSink sink) {
    super.indexStub(stub, sink);
    if (PuppetQualifiedNamesUtil.MAIN_NAMESPACE.equals(stub.getNamespaceName()) && !stub.isParameter()) {
      String name = stub.getName();
      assert name != null : "shouldCreateStub method should filter this";
      sink.occurrence(PuppetTopLevelVariablesStubsIndex.KEY, name);
    }
  }

  @Override
  public boolean shouldCreateStub(ASTNode node) {

    if (!super.shouldCreateStub(node)) {
      return false;
    }

    PsiElement psi = node.getPsi();
    return psi instanceof PuppetVariable &&
           ((PuppetVariable)psi).isDeclaration() &&
           (((PuppetVariable)psi).isParameter() || !((PuppetVariable)psi).isLexicalDeclaration());
  }
}
