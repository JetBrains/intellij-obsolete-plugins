package com.intellij.gwt.superSource;

import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.psi.GwtSourcePathsRefresher;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.gwt.model.impl.GwtSourcePath;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.intellij.util.containers.ContainerUtil.getFirstItem;

public final class GwtSuperSourceClassCacheImpl extends GwtSuperSourceClassCache implements Disposable {
  private final Project myProject;
  private final Map<String, Boolean> myCachedSuperSourceClasses = new ConcurrentHashMap<>();

  public GwtSuperSourceClassCacheImpl(Project project) {
    myProject = project;
  }

  @Override
  public void dispose() {
  }

  @Override
  public @NotNull List<PsiClass> findClassesByQualifiedName(@NotNull String qualifiedName,
                                                            @NotNull VirtualFile root) {
    List<PsiClass> result = new SmartList<>();
    List<String> innerClassNames = new SmartList<>();
    String currentName = qualifiedName;
    do {
      final VirtualFile file = root.findFileByRelativePath(currentName.replace('.', '/') + JavaFileType.DOT_DEFAULT_EXTENSION);
      if (file != null) {
        final PsiFile psiFile = getCachedPsiFile(file);
        if (psiFile instanceof PsiJavaFile) {
          for (PsiClass aClass : ((PsiJavaFile)psiFile).getClasses()) {
            if (currentName.equals(aClass.getQualifiedName())) {
              ContainerUtil.addIfNotNull(result, findInnerClass(aClass, innerClassNames));
            }
          }
        }
      }
      innerClassNames.add(StringUtil.getShortName(currentName));
      currentName = StringUtil.getPackageName(currentName);
    }
    while (!currentName.isEmpty());
    return result;
  }

  @Override
  public @Nullable PsiFile getCachedPsiFile(final VirtualFile file) {
    return PsiManager.getInstance(myProject).findFile(file);
  }

  @Override
  public boolean containsJreEmulationClass(@NotNull List<GwtModule> gwtModules, @NotNull String className) {
    Boolean result = myCachedSuperSourceClasses.get(className);
    if (result != null) {
      return result.booleanValue();
    }

    result = Boolean.FALSE;
    GwtSourcePathsRefresher sourcePathsRefresher = GwtSourcePathsRefresher.getInstance(myProject);
    for (GwtSourcePath superSourcePath : sourcePathsRefresher.getSuperSourcePaths(gwtModules)) {
      VirtualFile superSourceRoot = LocalFileSystem.getInstance().findFileByPath(superSourcePath.myFullPath);
      if (superSourceRoot == null) continue;

      PsiClass psiClass = getFirstItem(findClassesByQualifiedName(className, superSourceRoot));
      if (psiClass != null) {
        String filePath = psiClass.getContainingFile().getVirtualFile().getPath();
        if (sourcePathsRefresher.isIncluded(filePath, superSourcePath)) {
          result = Boolean.TRUE;
          break;
        }
      }
    }

    myCachedSuperSourceClasses.put(className, result);
    return result.booleanValue();
  }

  @Override
  public void invalidateSuperSourceCache() {
    myCachedSuperSourceClasses.clear();
  }
}
