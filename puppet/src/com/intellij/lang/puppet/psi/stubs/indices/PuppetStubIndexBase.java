package com.intellij.lang.puppet.psi.stubs.indices;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndex;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public abstract class PuppetStubIndexBase<Psi extends PsiElement> extends StringStubIndexExtension<Psi> {
  private static final int VERSION = 1;

  @Override
  public int getVersion() {
    return super.getVersion() + VERSION;
  }

  public Collection<Psi> find(@NotNull String key, PsiElement element) {
    return find(key, element.getProject(), element.getResolveScope());
  }

  public Collection<Psi> find(@NotNull String key, Project project, GlobalSearchScope scope) {
    return StubIndex.getElements(getKey(), key, project, scope, getClassToFetch());
  }

  public void processAllElements(@NotNull Project project, @NotNull PsiElement anchor, @NotNull PsiElementProcessor<Psi> processor) {
    for (String key : getAllKeys(project)) {
      for (Psi variable : find(key, anchor)) {
        processor.execute(variable);
      }
    }
  }

  protected abstract @NotNull Class<Psi> getClassToFetch();
}
