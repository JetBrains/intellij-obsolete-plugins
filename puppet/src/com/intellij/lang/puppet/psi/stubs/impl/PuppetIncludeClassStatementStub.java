package com.intellij.lang.puppet.psi.stubs.impl;

import com.intellij.lang.puppet.psi.PuppetIncludeClassStatement;
import com.intellij.lang.puppet.psi.stubs.PuppetStubElementBase;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class PuppetIncludeClassStatementStub extends PuppetStubElementBase<PuppetIncludeClassStatement> {

  private final @NotNull Collection<String> myClassNames;

  public PuppetIncludeClassStatementStub(StubElement parent,
                                         IStubElementType elementType,
                                         @NotNull Collection<String> classNames
  ) {
    super(parent, elementType);
    myClassNames = classNames;
  }

  public @NotNull Collection<String> getClassNames() {
    return myClassNames;
  }
}
