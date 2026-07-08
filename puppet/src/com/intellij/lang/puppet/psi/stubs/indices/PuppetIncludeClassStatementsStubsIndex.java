package com.intellij.lang.puppet.psi.stubs.indices;

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

public class PuppetIncludeClassStatementsStubsIndex extends StringStubIndexExtension<PsiElement> {
  private static final PuppetIncludeClassStatementsStubsIndex INSTANCE = new PuppetIncludeClassStatementsStubsIndex();

  public static final StubIndexKey<String, PsiElement> KEY = StubIndexKey.createIndexKey("puppet.class.instantiation");
  public static final int VERSION = 1;

  public static PuppetIncludeClassStatementsStubsIndex getInstance() {
    return INSTANCE;
  }

  @Override
  public int getVersion() {
    return super.getVersion() + VERSION;
  }

  @Override
  public @NotNull StubIndexKey<String, PsiElement> getKey() {
    return KEY;
  }

  public static Collection<PsiElement> find(@NotNull String key, PsiElement element) {
    return find(key, element.getProject(), element.getResolveScope());
  }

  public static Collection<PsiElement> find(@NonNls @NotNull String key, Project project, GlobalSearchScope scope) {
    return StubIndex.getElements(KEY, StringUtil.toLowerCase(key), project, scope, PsiElement.class);
  }
}
