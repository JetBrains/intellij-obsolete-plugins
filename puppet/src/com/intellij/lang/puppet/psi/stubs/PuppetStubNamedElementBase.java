package com.intellij.lang.puppet.psi.stubs;

import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PuppetStubNamedElementBase<T extends PsiNameIdentifierOwner> extends PuppetStubElementBase<T>
  implements PuppetStubNamedElement<T> {
  private final String myName;
  private final String myNamespaceName;

  public PuppetStubNamedElementBase(StubElement parent,
                                    IStubElementType elementType,
                                    @NotNull String name,
                                    @Nullable String namespaceName
  ) {
    super(parent, elementType);
    myName = name;
    myNamespaceName = namespaceName;
  }

  @Override
  public @Nullable String getName() {
    return myName;
  }

  @Override
  public @Nullable String getNamespaceName() {
    return myNamespaceName;
  }
}
