package com.intellij.lang.puppet.psi.stubs.indices;

import com.intellij.lang.puppet.psi.PuppetResourceInstanceDeclaration;
import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.annotations.NotNull;

public class PuppetResourceInstanceByTypeStubsIndex extends PuppetStubIndexBase<PuppetResourceInstanceDeclaration> {
  private static final PuppetResourceInstanceByTypeStubsIndex INSTANCE = new PuppetResourceInstanceByTypeStubsIndex();

  public static final StubIndexKey<String, PuppetResourceInstanceDeclaration> KEY = StubIndexKey.createIndexKey("puppet.resource.by.type");
  public static final int VERSION = 1;

  public static PuppetResourceInstanceByTypeStubsIndex getInstance() {
    return INSTANCE;
  }

  @Override
  public int getVersion() {
    return super.getVersion() + VERSION;
  }

  @Override
  protected @NotNull Class<PuppetResourceInstanceDeclaration> getClassToFetch() {
    return PuppetResourceInstanceDeclaration.class;
  }

  @Override
  public @NotNull StubIndexKey<String, PuppetResourceInstanceDeclaration> getKey() {
    return KEY;
  }
}
