package com.intellij.lang.puppet.psi.stubs;

import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;


public abstract class PuppetStubElementBase<T extends PsiElement> extends StubBase<T> implements PuppetStubElement<T> {
  protected PuppetStubElementBase(StubElement parent,
                                  IStubElementType elementType) {
    super(parent, elementType);
  }
}
