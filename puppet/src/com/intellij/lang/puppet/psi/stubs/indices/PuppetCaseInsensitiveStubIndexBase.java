package com.intellij.lang.puppet.psi.stubs.indices;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public abstract class PuppetCaseInsensitiveStubIndexBase<Psi extends PsiElement> extends PuppetStubIndexBase<Psi> {

  private static final int VERSION = 1;

  @Override
  public int getVersion() {
    return super.getVersion() + VERSION;
  }

  @Override
  public Collection<Psi> find(@NonNls @NotNull String key, Project project, GlobalSearchScope scope) {
    return super.find(StringUtil.toLowerCase(key), project, scope);
  }
}
