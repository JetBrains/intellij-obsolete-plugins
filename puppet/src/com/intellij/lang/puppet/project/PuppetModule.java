package com.intellij.lang.puppet.project;

import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.project.meta.PuppetHeadlessModuleMetadata;
import com.intellij.lang.puppet.project.meta.PuppetModuleMetadata;
import com.intellij.lang.puppet.project.meta.PuppetModuleMetadata.Dependency;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PuppetModule extends PuppetEntity<PuppetModuleMetadata> {
  public static final String DEPENDENCIES_DIR = ".dependencies";
  public static final String FIXTURES_FILE = ".fixtures.yml";
  public static final String FIXTURES_DEPENDENCIES_DIR = "spec/fixtures/modules";
  private static final List<String> MEANINGFUL_SUB_DIRECTORIES = Arrays.asList(
    "manifests",
    "functions",
    "lib",
    "facts.d"
  );

  private final Comparator<PuppetModule> MODULE_DEPENDENCY_COMPARATOR =
    (o1, o2) -> Boolean.compare(o1.isInternalDependency(), o2.isInternalDependency());

  public PuppetModule(@NotNull Project project,
                      @NotNull VirtualFile root,
                      @NotNull PuppetModuleMetadata metadata) {
    super(project, root, metadata);
  }

  @Override
  protected @NotNull List<String> getMeaningfulSubDirectoryNames() {
    return MEANINGFUL_SUB_DIRECTORIES;
  }

  /**
   * Checks if this module is internal dependency for environment or module
   */
  public boolean isInternalDependency() {
    PuppetEntity parentEntity = getContainingEntity();
    if (parentEntity == null) {
      return false;
    }
    return parentEntity.isUnderDependenciesRoot(getRoot());
  }

  /**
   * @return module short name, without puppetforge username
   */
  public @NotNull String getShortName() {
    return getRoot().getName();
  }

  /**
   * @return entity, containing current module or null
   */
  private @Nullable PuppetEntity getContainingEntity() {
    if (!isValid()) {
      return null;
    }
    VirtualFile parent = getRoot().getParent();
    if (parent == null) {
      return null;
    }
    return PuppetProjectModel.getInstance(getProject()).getPuppetModuleOrEnvironment(parent.getParent());
  }

  @Override
  public boolean isUnderDependenciesRoot(@NotNull VirtualFile virtualFile) {
    return isValid() && isUnderRoot(virtualFile, Arrays.asList(getLibrarianDependenciesRoot(), getFixturesDependenciesRoot()));
  }

  /**
   * @return true if module has no metadata file
   */
  public boolean isHeadless() {
    return getMetadata() instanceof PuppetHeadlessModuleMetadata;
  }

  @Override
  public @NotNull List<VirtualFile> getAllDependenciesRoots() {
    VirtualFile effectiveRoot = hasFixturesFile() ? getFixturesDependenciesRoot() : getLibrarianDependenciesRoot();
    return ContainerUtil.createMaybeSingletonList(effectiveRoot);
  }

  private @Nullable VirtualFile getFixturesDependenciesRoot() {
    return isValid() ? getRoot().findFileByRelativePath(FIXTURES_DEPENDENCIES_DIR) : null;
  }

  @Override
  public @NotNull String getLibrarianDependenciesRootName() {
    return DEPENDENCIES_DIR;
  }

  @Override
  public @NotNull String getDescriptiveName() {
    return PuppetBundle.message("puppet.module");
  }


  @Override
  public @NotNull List<PuppetModule> getDependencies() {
    if (hasFixturesFile()) {
      return getDependenciesFromFixtures();
    }
    if (getPuppetFile() != null) {
      return getDependenciesFromPuppetfile();
    }
    return getDependenciesFromMetadata();
  }

  private @NotNull List<PuppetModule> getDependenciesFromFixtures() {
    return PuppetProjectManager.getInstance(getProject()).getModulesInRoot(getFixturesDependenciesRoot());
  }

  private @NotNull List<PuppetModule> getDependenciesFromPuppetfile() {
    return PuppetProjectManager.getInstance(getProject()).getModulesInRoot(getLibrarianDependenciesRoot());
  }

  public boolean hasFixturesFile() {
    return isValid() && getRoot().findChild(FIXTURES_FILE) != null;
  }

  private @NotNull List<PuppetModule> getDependenciesFromMetadata() {
    PuppetModuleMetadata metadata = getMetadata();
    List<Dependency> dependencies = metadata.getDependencies();
    if (dependencies.isEmpty()) {
      return Collections.emptyList();
    }

    // fixme we should also check version
    Map<String, Dependency> dependencyMap = new HashMap<>();
    for (Dependency dependency : dependencies) {
      String dependencyName = dependency.getName();
      if (StringUtil.isNotEmpty(dependencyName)) {
        dependencyMap.putIfAbsent(dependencyName.replace('/', '-'), dependency);
      }
    }

    List<PuppetModule> projectModules = PuppetProjectModel.getInstance(getProject()).getAllModules();
    ContainerUtil.sort(projectModules, MODULE_DEPENDENCY_COMPARATOR);

    List<PuppetModule> result = new ArrayList<>();
    for (PuppetModule module : projectModules) {

      if (equals(module)) {
        continue;
      }

      if (dependencyMap.remove(module.getName()) != null) {
        result.add(module);
      }
    }
    return result;
  }

  @RequiresReadLock
  @Override
  @Nullable GlobalSearchScope calcResolveScope() {
    if (isInternalDependency()) {
      PuppetEntity containingEntity = getContainingEntity();
      assert containingEntity != null;
      return PuppetScopeManager.getInstance(getProject()).getResolveScope(containingEntity);
    }
    else {
      return super.calcResolveScope();
    }
  }
}
