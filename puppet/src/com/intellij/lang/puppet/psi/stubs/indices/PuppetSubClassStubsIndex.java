package com.intellij.lang.puppet.psi.stubs.indices;

import com.intellij.lang.puppet.psi.PuppetClassDefinition;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * This index contains classes by their parents
 */
public class PuppetSubClassStubsIndex extends StringStubIndexExtension<PuppetClassDefinition> {
  private static final PuppetSubClassStubsIndex INSTANCE = new PuppetSubClassStubsIndex();

  public static final StubIndexKey<String, PuppetClassDefinition> KEY = StubIndexKey.createIndexKey("puppet.subclass");
  public static final int VERSION = 1;

  public static PuppetSubClassStubsIndex getInstance() {
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

  public static Collection<PuppetClassDefinition> find(@NotNull String key, PsiElement element) {
    return find(key, element.getProject(), element.getResolveScope());
  }

  public static Collection<PuppetClassDefinition> find(@NonNls @NotNull String key, Project project, GlobalSearchScope scope) {
    return StubIndex.getElements(KEY, StringUtil.toLowerCase(key), project, scope, PuppetClassDefinition.class);
  }
}
