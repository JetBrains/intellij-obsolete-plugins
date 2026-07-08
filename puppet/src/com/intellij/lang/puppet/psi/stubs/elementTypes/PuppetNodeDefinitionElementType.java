package com.intellij.lang.puppet.psi.stubs.elementTypes;

import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.psi.PuppetNodeDefinition;
import com.intellij.lang.puppet.psi.impl.PsiPuppetNodeDefinitionImpl;
import com.intellij.lang.puppet.psi.stubs.PuppetNodeDefinitionStub;
import com.intellij.lang.puppet.psi.stubs.PuppetSerializationUtil;
import com.intellij.lang.puppet.psi.stubs.impl.PuppetNodeDefinitionStubImpl;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetNodeStubsIndex;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;


public class PuppetNodeDefinitionElementType extends PuppetStubElementType<PuppetNodeDefinitionStub, PuppetNodeDefinition> {
  public PuppetNodeDefinitionElementType(@NotNull @NonNls String debugName) {
    super(debugName);
  }

  @Override
  public PuppetNodeDefinition createPsi(@NotNull PuppetNodeDefinitionStub stub) {
    return new PsiPuppetNodeDefinitionImpl(stub, this);
  }

  @Override
  public @NotNull PuppetNodeDefinitionStub createStub(@NotNull PuppetNodeDefinition psi, StubElement parentStub) {
    return new PuppetNodeDefinitionStubImpl(parentStub, this, psi.getNamesList());
  }

  @Override
  public void serialize(@NotNull PuppetNodeDefinitionStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    PuppetSerializationUtil.serializeList(dataStream, stub.getNamesList());
  }

  @Override
  public @NotNull PuppetNodeDefinitionStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    List<String> namesList = PuppetSerializationUtil.deserializeList(dataStream);
    assert namesList != null;
    return new PuppetNodeDefinitionStubImpl(parentStub, this, namesList);
  }

  @Override
  public void indexStub(@NotNull PuppetNodeDefinitionStub stub, @NotNull IndexSink sink) {
    for (String name : stub.getNamesList()) {
      sink.occurrence(PuppetNodeStubsIndex.KEY, name);
    }
  }

  @Override
  public boolean shouldCreateStub(ASTNode node) {
    PsiElement psi = node.getPsi();
    if (!(psi instanceof PuppetNodeDefinition)) {
      return false;
    }

    List<String> namesList = ((PuppetNodeDefinition)psi).getNamesList();
    return !namesList.isEmpty();
  }
}
