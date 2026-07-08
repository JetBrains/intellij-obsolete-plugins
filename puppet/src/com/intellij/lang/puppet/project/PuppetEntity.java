package com.intellij.lang.puppet.project;

import com.intellij.lang.puppet.adapters.PuppetDependencyManagerAdapter;
import com.intellij.lang.puppet.ide.libraries.PuppetLibraryUtil;
import com.intellij.lang.puppet.project.meta.PuppetMetadata;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.util.ArrayUtil;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.intellij.openapi.util.NullableLazyValue.atomicLazyNullable;

public abstract class PuppetEntity<T extends PuppetMetadata> {
  private final @NotNull Project myProject;
  private final @NotNull VirtualFile myRoot;
  private final @NotNull T myMetadata;

  private final NullableLazyValue<Module> myModuleProvider = atomicLazyNullable(() -> ModuleUtilCore.findModuleForFile(getRoot(), getProject()));

  public PuppetEntity(@NotNull Project project, @NotNull VirtualFile root, @NotNull T metadata) {
    myProject = project;
    myRoot = root;
    myMetadata = metadata;
  }

  public @NotNull String getName() {
    String name = getMetadata().getName();
    return name == null ? getRoot().getName() : name;
  }

  public @Nullable Module getIdeaModule() {
    return myModuleProvider.getValue();
  }

  /**
   * @return directory name for librarian to install dependencies from PuppetFile/metadata.json
   */
  public abstract @NotNull String getLibrarianDependenciesRootName();

  /**
   * @return true if virtual file resides inside the dependencies directory (or potential dependencies directory)
   */
  public boolean isUnderDependenciesRoot(@NotNull VirtualFile virtualFile) {
    return isValid() && isUnderRoot(virtualFile, getAllDependenciesRoots());
  }

  /**
   * Checks if target file is under one of the provided roots
   */
  protected static boolean isUnderRoot(@NotNull VirtualFile target, @NotNull List<? extends VirtualFile> roots) {
    return VfsUtilCore.isUnder(target, new HashSet<>(roots));
  }

  /**
   * @return list of all dependencies roots for current entity
   */
  public abstract @NotNull List<VirtualFile> getAllDependenciesRoots();

  protected @Nullable VirtualFile getLibrarianDependenciesRoot() {
    if (!isValid()) {
      return null;
    }
    return myRoot.findChild(getLibrarianDependenciesRootName());
  }

  /**
   * @return true if model is ok
   */
  @RequiresReadLock
  public boolean isValid() {
    return myRoot.isValid() && !myProject.isDisposed();
  }

  /**
   * @return 'module' or 'environment'
   */
  public abstract @NotNull String getDescriptiveName();

  /**
   * @return list of modules this entity depends on
   */
  public abstract @NotNull List<PuppetModule> getDependencies();

  public @NotNull Project getProject() {
    return myProject;
  }

  public @NotNull VirtualFile getRoot() {
    return myRoot;
  }

  public @NotNull T getMetadata() {
    return myMetadata;
  }

  public GlobalSearchScope getResolveScope() {
    return PuppetScopeManager.getInstance(myProject).getResolveScope(this);
  }

  /**
   * @return list of directory names with manifests, libs and so on, to be included into resolve scope
   */
  protected abstract @NotNull List<String> getMeaningfulSubDirectoryNames();

  /**
   * @return Puppetfile virtual file for this entity
   */
  public @Nullable VirtualFile getPuppetFile() {
    return isValid() ? getRoot().findChild(PuppetProjectManager.PUPPET_FILE) : null;
  }

  /**
   * Returns dependency manager adapter for current entity, see {@link PuppetDependencyManagerAdapter}
   *
   * @return PuppetDependencyManagerAdapter or null if not available
   */
  public @Nullable PuppetDependencyManagerAdapter getDependencyManager() {
    for (PuppetDependencyManagerAdapter adapter : PuppetDependencyManagerAdapter.getExtensions()) {
      if (adapter.isApplicable(this)) {
        return adapter;
      }
    }
    return null;
  }

  /**
   * @return entity with dependencies resolve scope
   */
  @RequiresReadLock
  @Nullable GlobalSearchScope calcResolveScope() {
    GlobalSearchScope baseScope = getResolveScopeWithoutDependencies();
    List<PuppetModule> dependencies = getDependencies();
    if (!dependencies.isEmpty()) {
      GlobalSearchScope[] scopes = dependencies.stream()
        .map(dependency -> ((PuppetEntity<?>)dependency).getResolveScopeWithoutDependencies())
        .filter(scope -> scope != GlobalSearchScope.EMPTY_SCOPE)
        .toArray(GlobalSearchScope[]::new);
      baseScope = GlobalSearchScope.union(ArrayUtil.append(scopes, baseScope));
    }

    VirtualFile stubsRoot = PuppetLibraryUtil.getStubsRoot();
    return stubsRoot == null ? baseScope : baseScope.uniteWith(GlobalSearchScopesCore.directoryScope(myProject, stubsRoot, true));
  }

  /**
   * @return resolve scope for current puppet entity
   */
  @RequiresReadLock
  private @NotNull GlobalSearchScope getResolveScopeWithoutDependencies() {
    if (!isValid()) {
      return GlobalSearchScope.EMPTY_SCOPE;
    }

    final List<VirtualFile> subDirs = new ArrayList<>();

    for (String dirName : getMeaningfulSubDirectoryNames()) {
      VirtualFile child = myRoot.findChild(dirName);
      if (child != null) {
        subDirs.add(child);
      }
    }
    return subDirs.isEmpty()
           ? GlobalSearchScope.EMPTY_SCOPE
           : GlobalSearchScopesCore.directoriesScope(myProject, true, subDirs.toArray(VirtualFile.EMPTY_ARRAY));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PuppetEntity<?> entity = (PuppetEntity<?>)o;

    if (!myProject.equals(entity.myProject)) return false;
    if (!myRoot.equals(entity.myRoot)) return false;
    if (!myMetadata.equals(entity.myMetadata)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myProject.hashCode();
    result = 31 * result + myRoot.hashCode();
    result = 31 * result + myMetadata.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + myMetadata.getPresentableName() + "; " + myRoot + "]";
  }
}
