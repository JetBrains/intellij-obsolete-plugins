package com.intellij.lang.puppet.psi.stubs.indices;

import com.intellij.lang.puppet.psi.PuppetFunctionDefinition;
import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.annotations.NotNull;

public class PuppetFunctionsStubsIndex extends PuppetStubIndexBase<PuppetFunctionDefinition> {
  private static final PuppetFunctionsStubsIndex INSTANCE = new PuppetFunctionsStubsIndex();

  public static final StubIndexKey<String, PuppetFunctionDefinition> KEY = StubIndexKey.createIndexKey("puppet.function");
  private static final int ourVersion = 1;

  public static PuppetFunctionsStubsIndex getInstance() {
    return INSTANCE;
  }


  @Override
  public int getVersion() {
    return super.getVersion() + ourVersion;
  }

  @Override
  public @NotNull StubIndexKey<String, PuppetFunctionDefinition> getKey() {
    return KEY;
  }

  @Override
  protected @NotNull Class<PuppetFunctionDefinition> getClassToFetch() {
    return PuppetFunctionDefinition.class;
  }
}
