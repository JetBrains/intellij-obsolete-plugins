package com.intellij.lang.puppet.psi.stubs.impl;

import com.intellij.lang.puppet.psi.PuppetPolyNamedPsiElement;
import com.intellij.lang.puppet.psi.stubs.PuppetPolyNamedStubElement;
import com.intellij.lang.puppet.psi.stubs.PuppetStubElementBase;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class PuppetPolyNamedStubElementBase<T extends PuppetPolyNamedPsiElement> extends PuppetStubElementBase<T> implements
                                                                                                                        PuppetPolyNamedStubElement<T> {
  private final @NotNull List<String> myNamesList;

  public PuppetPolyNamedStubElementBase(StubElement parent, IStubElementType elementType, @NotNull List<String> namesList) {
    super(parent, elementType);
    myNamesList = namesList;
  }

  @Override
  public @NotNull List<String> getNamesList() {
    return myNamesList;
  }
}
