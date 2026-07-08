package com.intellij.lang.puppet.psi.stubs.elementTypes;

import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.psi.PuppetIncludeClassStatement;
import com.intellij.lang.puppet.psi.stubs.impl.PuppetIncludeClassStatementStub;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetIncludeClassStatementsStubsIndex;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public abstract class PuppetIncludeClassStatementElementTypeBase<T extends PuppetIncludeClassStatement>
  extends PuppetStubElementType<PuppetIncludeClassStatementStub, T> {

  public PuppetIncludeClassStatementElementTypeBase(@NotNull @NonNls String debugName) {
    super(debugName);
  }

  @Override
  public void serialize(@NotNull PuppetIncludeClassStatementStub stub, @NotNull StubOutputStream dataStream) {
    // do nothing
  }

  @Override
  public @NotNull PuppetIncludeClassStatementStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) {
    return new PuppetIncludeClassStatementStub(parentStub, this, Collections.emptyList());
  }

  @Override
  public void indexStub(@NotNull PuppetIncludeClassStatementStub stub, @NotNull IndexSink sink) {
    for (String className : stub.getClassNames()) {
      sink.occurrence(PuppetIncludeClassStatementsStubsIndex.KEY, className);
    }
  }

  @Override
  public @NotNull PuppetIncludeClassStatementStub createStub(@NotNull T psi, StubElement<?> parentStub) {
    return new PuppetIncludeClassStatementStub(parentStub, this, psi.getClassNames());
  }

  @Override
  public boolean shouldCreateStub(ASTNode node) {
    PsiElement psi = node.getPsi();
    return psi instanceof PuppetIncludeClassStatement && !((PuppetIncludeClassStatement)psi).getClassNames().isEmpty();
  }
}
