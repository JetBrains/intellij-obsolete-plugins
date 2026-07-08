package com.intellij.lang.puppet.psi.stubs.indices;

import com.intellij.lang.puppet.psi.PuppetResourceInstanceDeclaration;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;


public class PuppetResourceInstanceStubsIndex extends PuppetStubIndexBase<PuppetResourceInstanceDeclaration> {

  private static final PuppetResourceInstanceStubsIndex INSTANCE = new PuppetResourceInstanceStubsIndex();

  public static final StubIndexKey<String, PuppetResourceInstanceDeclaration> KEY = StubIndexKey.createIndexKey("puppet.resource");
  public static final int VERSION = 5;

  public static PuppetResourceInstanceStubsIndex getInstance() {
    return INSTANCE;
  }

  @Override
  public int getVersion() {
    return super.getVersion() + VERSION;
  }

  @Override
  public @NotNull StubIndexKey<String, PuppetResourceInstanceDeclaration> getKey() {
    return KEY;
  }

  @Override
  protected @NotNull Class<PuppetResourceInstanceDeclaration> getClassToFetch() {
    return PuppetResourceInstanceDeclaration.class;
  }

  public static Collection<PuppetResourceInstanceDeclaration> find(@Nullable String resourceName,
                                                                   @NotNull String typeName,
                                                                   @NotNull PsiElement element) {
    if (resourceName == null) {
      return Collections.emptyList();
    }
    return getInstance()
      .find(typeName + PuppetResourceInstanceDeclaration.SEPARATOR + resourceName, element.getProject(), element.getResolveScope());
  }
}
