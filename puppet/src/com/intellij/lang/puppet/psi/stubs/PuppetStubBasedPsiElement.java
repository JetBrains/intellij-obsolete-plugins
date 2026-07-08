package com.intellij.lang.puppet.psi.stubs;

import com.intellij.lang.puppet.psi.PuppetCompositePsiElement;
import com.intellij.psi.StubBasedPsiElement;


public interface PuppetStubBasedPsiElement<S extends PuppetStubElement> extends
                                                                        StubBasedPsiElement<S>,
                                                                        PuppetCompositePsiElement {
}
