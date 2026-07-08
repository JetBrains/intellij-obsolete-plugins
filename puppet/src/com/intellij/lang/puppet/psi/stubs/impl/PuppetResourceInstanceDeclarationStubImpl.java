package com.intellij.lang.puppet.psi.stubs.impl;

import com.intellij.lang.puppet.psi.PuppetResourceInstanceDeclaration;
import com.intellij.lang.puppet.psi.stubs.PuppetResourceInstanceDeclarationStub;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class PuppetResourceInstanceDeclarationStubImpl extends PuppetPolyNamedStubElementBase<PuppetResourceInstanceDeclaration>
  implements PuppetResourceInstanceDeclarationStub {
  private final @NotNull String myFullQualifiedTypeName;

  private final boolean myIsVirtual;

  private final boolean myIsExported;

  public PuppetResourceInstanceDeclarationStubImpl(StubElement parent,
                                                   IStubElementType elementType,
                                                   @NotNull List<String> namesList,
                                                   @NotNull String fullQualifiedTypeName,
                                                   boolean isVirtual,
                                                   boolean isExported
  ) {
    super(parent, elementType, namesList);

    myFullQualifiedTypeName = fullQualifiedTypeName;
    myIsVirtual = isVirtual;
    myIsExported = isExported;
  }

  @Override
  public @NotNull String getEffectiveTypeName() {
    return myFullQualifiedTypeName;
  }

  @Override
  public boolean isExported() {
    return myIsExported;
  }

  @Override
  public boolean isVirtual() {
    return myIsVirtual;
  }
}
