package com.intellij.lang.puppet.psi;

import com.intellij.lang.puppet.psi.stubs.PuppetStubBasedPsiElement;
import com.intellij.lang.puppet.psi.stubs.impl.PuppetIncludeClassStatementStub;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface PuppetIncludeClassStatement extends PuppetStubBasedPsiElement<PuppetIncludeClassStatementStub> {
  @NotNull
  Collection<String> getClassNames();
}
