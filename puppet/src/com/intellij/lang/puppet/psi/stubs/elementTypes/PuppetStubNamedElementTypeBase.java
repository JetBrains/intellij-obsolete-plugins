package com.intellij.lang.puppet.psi.stubs.elementTypes;

import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.psi.PuppetCompositePsiElement;
import com.intellij.lang.puppet.psi.stubs.PuppetStubBasedNamedPsiElement;
import com.intellij.lang.puppet.psi.stubs.PuppetStubNamedElement;
import com.intellij.openapi.util.text.StringUtil;
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

public abstract class PuppetStubNamedElementTypeBase<S extends PuppetStubNamedElement<?>, T extends PuppetCompositePsiElement>
  extends PuppetStubElementType<S, T> {
  public PuppetStubNamedElementTypeBase(@NotNull @NonNls String debugName) {
    super(debugName);
  }

  @Override
  public void indexStub(@NotNull S stub, @NotNull IndexSink sink) {
    String fullQualifiedName = stub.getFullQualifiedName();
    assert fullQualifiedName != null : "shouldCreateStub method should filter this";
    sink.occurrence(getIndexKey(), fullQualifiedName);
  }

  /**
   * Should return the key for fullQualifiedName indexing
   *
   */
  protected abstract StubIndexKey<String, T> getIndexKey();

  @Override
  public final void serialize(@NotNull S stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getName());
    dataStream.writeName(stub.getNamespaceName());
    serializeRest(stub, dataStream);
  }

  /**
   * To serialize additional information, you should override this method, to preserve serialization order
   *
   */
  protected void serializeRest(@NotNull S stub, @NotNull StubOutputStream dataStream) throws IOException {
  }

  @Override
  public final @NotNull S deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    String name = dataStream.readNameString();
    assert name != null : "shouldCreateStub method should filter this";
    return deserializeRest(dataStream, parentStub, name, dataStream.readNameString());
  }

  /**
   * Implementation should read the rest of serialized information if necessary and create a stub from fqn name provided
   *
   * @return stub object
   */
  protected abstract S deserializeRest(@NotNull StubInputStream dataStream,
                                       StubElement<?> parentStub,
                                       @NotNull String name,
                                       @Nullable String namespaceName
  ) throws IOException;

  @Override
  public boolean shouldCreateStub(ASTNode node) {
    PsiElement psi = node.getPsi();
    return psi instanceof PuppetStubBasedNamedPsiElement &&
           StringUtil.isNotEmpty(((PuppetStubBasedNamedPsiElement<?>)psi).getFullQualifiedName());
  }
}
