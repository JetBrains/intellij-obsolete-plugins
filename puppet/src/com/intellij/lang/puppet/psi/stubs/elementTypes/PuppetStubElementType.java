package com.intellij.lang.puppet.psi.stubs.elementTypes;

import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.psi.PuppetCompositePsiElement;
import com.intellij.lang.puppet.psi.stubs.PuppetStubElement;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public abstract class PuppetStubElementType<S extends PuppetStubElement<?>, T extends PuppetCompositePsiElement>
  extends IStubElementType<S, T> {

  public PuppetStubElementType(@NotNull @NonNls String debugName) {
    super(debugName, PuppetLanguage.INSTANCE);
  }

  @Override
  public String toString() {
    return "Puppet: " + super.toString();
  }

  @Override
  public final @NotNull String getExternalId() {
    return "puppet." + getDebugName();
  }
}
