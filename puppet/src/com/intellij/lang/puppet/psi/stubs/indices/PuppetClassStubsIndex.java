package com.intellij.lang.puppet.psi.stubs.indices;

import com.intellij.lang.puppet.psi.PuppetClassDefinition;
import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.annotations.NotNull;


public class PuppetClassStubsIndex extends PuppetCaseInsensitiveStubIndexBase<PuppetClassDefinition> {
  private static final PuppetClassStubsIndex INSTANCE = new PuppetClassStubsIndex();

  public static final StubIndexKey<String, PuppetClassDefinition> KEY = StubIndexKey.createIndexKey("puppet.class");
  public static final int VERSION = 3;

  public static PuppetClassStubsIndex getInstance() {
    return INSTANCE;
  }

  @Override
  public int getVersion() {
    return super.getVersion() + VERSION;
  }

  @Override
  public @NotNull StubIndexKey<String, PuppetClassDefinition> getKey() {
    return KEY;
  }

  @Override
  protected @NotNull Class<PuppetClassDefinition> getClassToFetch() {
    return PuppetClassDefinition.class;
  }
}
