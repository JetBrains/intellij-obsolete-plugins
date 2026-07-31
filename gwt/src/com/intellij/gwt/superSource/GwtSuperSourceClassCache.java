package com.intellij.gwt.superSource;

import com.intellij.gwt.module.model.GwtModule;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class GwtSuperSourceClassCache {
  public abstract @NotNull List<PsiClass> findClassesByQualifiedName(@NotNull String qualifiedName, @NotNull VirtualFile root);

  public static GwtSuperSourceClassCache getInstance(@NotNull Project project) {
    return project.getService(GwtSuperSourceClassCache.class);
  }

  public abstract @Nullable PsiFile getCachedPsiFile(VirtualFile file);

  public abstract boolean containsJreEmulationClass(@NotNull List<GwtModule> gwtModules, @NotNull String className);

  public abstract void invalidateSuperSourceCache();

  public static @Nullable PsiClass findInnerClass(PsiClass topLevelClass, List<String> names) {
    PsiClass current = topLevelClass;
    for (int i = names.size() - 1; i >= 0 && current != null; i--) {
      String name = names.get(i);
      current = current.findInnerClassByName(name, false);
    }
    return current;
  }
}
