package com.intellij.lang.puppet.psi.stubs;

import com.intellij.lang.puppet.psi.PuppetFullQualifiedNameOwner;
import com.intellij.psi.PsiNameIdentifierOwner;

public interface PuppetStubBasedNamedPsiElement<S extends PuppetStubElement> extends PuppetStubBasedPsiElement<S>,
                                                                                     PuppetFullQualifiedNameOwner,
                                                                                     PsiNameIdentifierOwner {
}
