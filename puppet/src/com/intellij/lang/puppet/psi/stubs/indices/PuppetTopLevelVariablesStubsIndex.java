package com.intellij.lang.puppet.psi.stubs.indices;

import com.intellij.lang.puppet.psi.PuppetVariable;
import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.annotations.NotNull;

public class PuppetTopLevelVariablesStubsIndex extends PuppetStubIndexBase<PuppetVariable> {
  private static final PuppetTopLevelVariablesStubsIndex INSTANCE = new PuppetTopLevelVariablesStubsIndex();

  public static final StubIndexKey<String, PuppetVariable> KEY = StubIndexKey.createIndexKey("puppet.top.variable");
  public static final int VERSION = 2;

  public static PuppetTopLevelVariablesStubsIndex getInstance() {
    return INSTANCE;
  }

  @Override
  public int getVersion() {
    return super.getVersion() + VERSION;
  }

  @Override
  public @NotNull StubIndexKey<String, PuppetVariable> getKey() {
    return KEY;
  }

  @Override
  protected @NotNull Class<PuppetVariable> getClassToFetch() {
    return PuppetVariable.class;
  }
}
