package com.intellij.lang.puppet.psi.stubs.indices;

import com.intellij.lang.puppet.ide.libraries.PuppetLibraryUtil;
import com.intellij.lang.puppet.psi.PuppetTypeDefinition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class PuppetTypeStubIndex extends PuppetCaseInsensitiveStubIndexBase<PuppetTypeDefinition> {
  private static final PuppetTypeStubIndex INSTANCE = new PuppetTypeStubIndex();

  public static final StubIndexKey<String, PuppetTypeDefinition> KEY = StubIndexKey.createIndexKey("puppet.type");
  public static final int VERSION = 1;

  public static PuppetTypeStubIndex getInstance() {
    return INSTANCE;
  }

  @Override
  public int getVersion() {
    return super.getVersion() + VERSION;
  }

  @Override
  public @NotNull StubIndexKey<String, PuppetTypeDefinition> getKey() {
    return KEY;
  }

  @Override
  protected @NotNull Class<PuppetTypeDefinition> getClassToFetch() {
    return PuppetTypeDefinition.class;
  }

  public static Collection<PuppetTypeDefinition> getMetaparametersContainingTypes(@NotNull PsiElement scopeProvider) {
    return getInstance().find(PuppetLibraryUtil.PUPPET_METAPARAMETERS_STUB_TYPE_NAME, scopeProvider);
  }
}
