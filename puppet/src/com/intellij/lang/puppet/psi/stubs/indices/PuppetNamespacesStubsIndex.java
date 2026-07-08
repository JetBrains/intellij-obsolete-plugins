package com.intellij.lang.puppet.psi.stubs.indices;

import com.intellij.lang.puppet.psi.PuppetNamespaceDefinition;
import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.annotations.NotNull;

public class PuppetNamespacesStubsIndex extends PuppetCaseInsensitiveStubIndexBase<PuppetNamespaceDefinition> {
  public static final StubIndexKey<String, PuppetNamespaceDefinition> KEY = StubIndexKey.createIndexKey("puppet.namespace");
  private static final int VERSION = 1;

  private static final PuppetNamespacesStubsIndex INSTANCE = new PuppetNamespacesStubsIndex();

  public static PuppetNamespacesStubsIndex getInstance() {return INSTANCE;}

  @Override
  public int getVersion() {
    return super.getVersion() + VERSION;
  }

  @Override
  public @NotNull StubIndexKey<String, PuppetNamespaceDefinition> getKey() {
    return KEY;
  }

  @Override
  protected @NotNull Class<PuppetNamespaceDefinition> getClassToFetch() {
    return PuppetNamespaceDefinition.class;
  }
}
