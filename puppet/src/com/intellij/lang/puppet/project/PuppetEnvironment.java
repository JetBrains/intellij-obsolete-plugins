package com.intellij.lang.puppet.project;

import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.project.meta.PuppetEnvironmentMetadata;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.intellij.openapi.util.NullableLazyValue.atomicLazyNullable;

public class PuppetEnvironment extends PuppetEntity<PuppetEnvironmentMetadata> {
  public static final String DEFAULT_DEPENDENCIES_DIR = "modules";
  private static final List<String> DEFAULT_DEPENDENCIES_DIR_LIST = Collections.singletonList(DEFAULT_DEPENDENCIES_DIR);
  private static final String MODULE_PATH_KEY = "modulepath";
  private static final List<String> MEANINGFUL_SUB_DIRECTORIES = Collections.singletonList("manifests");

  private final NullableLazyValue<Properties> myPropertiesProvider = atomicLazyNullable(() -> ReadAction.compute(() -> computeEnvironmentProperties()));

  public PuppetEnvironment(@NotNull Project project,
                           @NotNull VirtualFile root) {
    super(project, root, new PuppetEnvironmentMetadata(root));
  }

  @Override
  protected @NotNull List<String> getMeaningfulSubDirectoryNames() {
    return MEANINGFUL_SUB_DIRECTORIES;
  }

  @Override
  public @NotNull String getDescriptiveName() {
    return PuppetBundle.message("puppet.environment");
  }

  @Override
  public @NotNull String getLibrarianDependenciesRootName() {
    List<String> paths = getAllDependenciesRootsPaths();
    return paths.isEmpty() ? DEFAULT_DEPENDENCIES_DIR : paths.get(0);
  }

  @RequiresReadLock
  @Override
  public @NotNull List<VirtualFile> getAllDependenciesRoots() {
    if (!isValid()) {
      return Collections.emptyList();
    }
    return getAllDependenciesRootsPaths().stream()
      .map(path -> getRoot().findFileByRelativePath(path))
      .filter(file -> file != null)
      .collect(Collectors.toList());
  }

  /**
   * @return list of dependencies relative paths from the environment.conf or list with default modules
   */
  private @NotNull List<String> getAllDependenciesRootsPaths() {
    Properties environmentProperties = getEnvironmentProperties();
    if (environmentProperties == null) {
      return DEFAULT_DEPENDENCIES_DIR_LIST;
    }
    String userDefinedModulePath = environmentProperties.getProperty(MODULE_PATH_KEY);
    if (userDefinedModulePath == null) {
      return DEFAULT_DEPENDENCIES_DIR_LIST;
    }
    return StringUtil.split(userDefinedModulePath, ":");
  }

  /**
   * @return list of modules from dependencies directories
   */
  @Override
  public @NotNull List<PuppetModule> getDependencies() {
    PuppetProjectManager puppetProjectManager = PuppetProjectManager.getInstance(getProject());
    return getAllDependenciesRoots().stream()
      .flatMap(root -> puppetProjectManager.getModulesInRoot(root).stream())
      .collect(Collectors.toList());
  }

  private @Nullable Properties getEnvironmentProperties() {
    return myPropertiesProvider.getValue();
  }

  private @Nullable Properties computeEnvironmentProperties() {
    return buildPropertiesFromConfig(readEnvironmentConfig());
  }

  /**
   * @return environment.conf contents or null if something went wrong
   */
  private @Nullable String readEnvironmentConfig() {
    if (!isValid()) {
      return null;
    }

    VirtualFile puppetMetaFile = getRoot().findChild(PuppetProjectManager.ENVIRONMENT_META_FILE);
    if (puppetMetaFile == null) {
      return null;
    }

    try {
      return VfsUtilCore.loadText(puppetMetaFile);
    }
    catch (IOException e) {
      return null;
    }
  }

  /*
   * @param configContents configuration file contents
   * @return Properties object or null is something went wrong
   */
  private static @Nullable Properties buildPropertiesFromConfig(@Nullable String configContents) {
    if (configContents == null) {
      return null;
    }
    Properties result = new Properties();
    try {
      result.load(new StringReader(configContents));
    }
    catch (IOException e) {
      return null;
    }
    return result;
  }
}
