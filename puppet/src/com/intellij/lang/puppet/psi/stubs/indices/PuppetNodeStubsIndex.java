package com.intellij.lang.puppet.psi.stubs.indices;

import com.intellij.lang.puppet.psi.PuppetNodeDefinition;
import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.annotations.NotNull;

public class PuppetNodeStubsIndex extends PuppetStubIndexBase<PuppetNodeDefinition> {
  private static final PuppetNodeStubsIndex INSTANCE = new PuppetNodeStubsIndex();

  public static final StubIndexKey<String, PuppetNodeDefinition> KEY = StubIndexKey.createIndexKey("puppet.node");
  public static final int VERSION = 1;

  @Override
  public int getVersion() {
    return super.getVersion() + VERSION;
  }

  public static PuppetNodeStubsIndex getInstance() {
    return INSTANCE;
  }

  @Override
  public @NotNull StubIndexKey<String, PuppetNodeDefinition> getKey() {
    return KEY;
  }

  @Override
  protected @NotNull Class<PuppetNodeDefinition> getClassToFetch() {
    return PuppetNodeDefinition.class;
  }
}
