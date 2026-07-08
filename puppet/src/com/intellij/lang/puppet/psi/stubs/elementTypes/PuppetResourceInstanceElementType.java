package com.intellij.lang.puppet.psi.stubs.elementTypes;

import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.psi.PuppetResourceInstanceDeclaration;
import com.intellij.lang.puppet.psi.impl.PsiPuppetResourceInstanceDeclarationImpl;
import com.intellij.lang.puppet.psi.stubs.PuppetResourceInstanceDeclarationStub;
import com.intellij.lang.puppet.psi.stubs.PuppetSerializationUtil;
import com.intellij.lang.puppet.psi.stubs.impl.PuppetResourceInstanceDeclarationStubImpl;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetResourceInstanceByTypeStubsIndex;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetResourceInstanceStubsIndex;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

import static com.intellij.lang.puppet.psi.PuppetResourceInstanceDeclaration.SEPARATOR;

public class PuppetResourceInstanceElementType
  extends PuppetStubElementType<PuppetResourceInstanceDeclarationStub, PuppetResourceInstanceDeclaration> {
  public PuppetResourceInstanceElementType(@NotNull @NonNls String debugName) {
    super(debugName);
  }

  @Override
  public PuppetResourceInstanceDeclaration createPsi(@NotNull PuppetResourceInstanceDeclarationStub stub) {
    return new PsiPuppetResourceInstanceDeclarationImpl(stub, this);
  }

  @Override
  public @NotNull PuppetResourceInstanceDeclarationStub createStub(@NotNull PuppetResourceInstanceDeclaration psi, StubElement parentStub) {
    String effectiveTypeName = psi.getEffectiveTypeName();
    assert effectiveTypeName != null : "shouldCreateStub should filter out this";
    return new PuppetResourceInstanceDeclarationStubImpl(
      parentStub,
      this,
      psi.getNamesList(),
      effectiveTypeName,
      psi.isVirtual(),
      psi.isExported()
    );
  }

  @Override
  public void serialize(@NotNull PuppetResourceInstanceDeclarationStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    PuppetSerializationUtil.serializeList(dataStream, stub.getNamesList());
    dataStream.writeName(stub.getEffectiveTypeName());
    dataStream.writeBoolean(stub.isVirtual());
    dataStream.writeBoolean(stub.isExported());
  }

  @Override
  public @NotNull PuppetResourceInstanceDeclarationStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub)
    throws IOException {
    List<String> namesList = PuppetSerializationUtil.deserializeList(dataStream);
    assert namesList != null : "shouldCreateStub method should filter this";
    String qualifiedName = dataStream.readNameString();
    assert qualifiedName != null : "shouldCreateStub method should filter this";
    return new PuppetResourceInstanceDeclarationStubImpl(
      parentStub,
      this,
      namesList,
      qualifiedName,
      dataStream.readBoolean(),
      dataStream.readBoolean()
    );
  }

  @Override
  public void indexStub(@NotNull PuppetResourceInstanceDeclarationStub stub, @NotNull IndexSink sink) {

    String typeName = stub.getEffectiveTypeName();
    for (String name : stub.getNamesList()) {
      sink.occurrence(PuppetResourceInstanceStubsIndex.KEY, typeName + SEPARATOR + name);
    }
    sink.occurrence(PuppetResourceInstanceByTypeStubsIndex.KEY, typeName);
  }

  @Override
  public boolean shouldCreateStub(ASTNode node) {
    PsiElement psi = node.getPsi();
    if (!(psi instanceof PuppetResourceInstanceDeclaration)) {
      return false;
    }

    List<String> namesList = ((PuppetResourceInstanceDeclaration)psi).getNamesList();

    return !namesList.isEmpty() && StringUtil.isNotEmpty(((PuppetResourceInstanceDeclaration)psi).getEffectiveTypeName());
  }
}
