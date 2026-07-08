package com.intellij.lang.puppet.psi;

import com.intellij.lang.puppet.psi.stubs.PuppetPolyNamedStubElement;
import com.intellij.lang.puppet.psi.stubs.PuppetStubBasedPsiElement;

public interface PuppetStubBasedPolyNamedPsiElement<S extends PuppetPolyNamedStubElement>
  extends PuppetStubBasedPsiElement<S>, PuppetPolyNamedPsiElement {
}
