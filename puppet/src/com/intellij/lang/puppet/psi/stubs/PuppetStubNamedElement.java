package com.intellij.lang.puppet.psi.stubs;

import com.intellij.lang.puppet.psi.PuppetFullQualifiedNameOwner;
import com.intellij.psi.PsiNameIdentifierOwner;

public interface PuppetStubNamedElement<T extends PsiNameIdentifierOwner> extends PuppetStubElement<T>,
                                                                                  PuppetFullQualifiedNameOwner {
}
