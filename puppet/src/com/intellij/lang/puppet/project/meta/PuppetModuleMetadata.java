package com.intellij.lang.puppet.project.meta;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class PuppetModuleMetadata implements PuppetMetadata {
  private static final Gson myGson = new Gson();

  // following fields names conforms fields names in json file and being filled using reflection
  @Expose
  private @Nullable String name;
  @Expose
  private @Nullable String version;
  @Expose
  private @Nullable String author;
  @Expose
  private @Nullable String license;
  @Expose
  private @Nullable String summary;
  @Expose
  private @Nullable String source;
  @Expose
  private @Nullable List<Dependency> dependencies;
  @Expose
  private @Nullable String project_page;
  @Expose
  private @Nullable String issues_url;
  @Expose
  private @Nullable List<OsSupport> operatingsystem_support;
  @Expose
  private @Nullable List<String> tags;
  @Expose
  private @Nullable String data_provider;

  private final transient NotNullLazyValue<List<Dependency>> myDependencyProvider = NotNullLazyValue.atomicLazy(() -> {
    if (dependencies == null) {
      return Collections.emptyList();
    }
    return ContainerUtil.filter(dependencies, (dependency -> dependency != null));
  });

  @Override
  public String toString() {
    return getName() + " " + getVersion();
  }

  @Override
  public @Nullable String getName() {
    return name;
  }

  protected void setName(@Nullable String newName) {
    name = newName;
  }

  @Override
  public @NotNull String getPresentableName() {
    String moduleName = getName();
    String moduleVersion = getVersion();

    if (StringUtil.isEmpty(moduleName)) {
      return PuppetBundle.message("puppet.module.name.unnamed");
    }
    else if (StringUtil.isEmpty(moduleVersion)) {
      moduleVersion = PuppetBundle.message("puppet.module.unknown.version");
    }

    return PuppetBundle.message("puppet.module.name", moduleName, moduleVersion);
  }

  public @Nullable String getVersion() {
    return version;
  }

  public @Nullable String getAuthor() {
    return author;
  }

  public @Nullable String getLicense() {
    return license;
  }

  public @Nullable String getSummary() {
    return summary;
  }

  public @Nullable String getSource() {
    return source;
  }

  public @NotNull List<Dependency> getDependencies() {
    return myDependencyProvider.getValue();
  }

  public @Nullable String getProjectPage() {
    return project_page;
  }

  public @Nullable String getIssuesUrl() {
    return issues_url;
  }

  public @NotNull List<OsSupport> getOperatingSystemSupport() {
    return operatingsystem_support == null ? Collections.emptyList() : operatingsystem_support;
  }

  public @NotNull List<String> getTags() {
    return tags == null ? Collections.emptyList() : tags;
  }

  public @Nullable String getDataProvider() {
    return data_provider;
  }

  public static PuppetModuleMetadata readMetadata(@NotNull VirtualFile sourceFile) {
    try {
      return myGson.fromJson(new String(sourceFile.contentsToByteArray(false), StandardCharsets.UTF_8), PuppetModuleMetadata.class);
    }
    catch (Exception e) {
      return null;
    }
  }

  public static final class Dependency {
    private @Nullable String name;
    private @Nullable String version_requirement;

    public @Nullable String getName() {
      return name;
    }

    public @Nullable String getVersionRequirement() {
      return version_requirement;
    }
  }

  public static class OsSupport {
    private @Nullable String operatingsystem;
    private @Nullable List<String> operatingsystemrelease;

    public @Nullable String getOperatingSystem() {
      return operatingsystem;
    }

    public @NotNull List<String> getOperatingSystemRelease() {
      return operatingsystemrelease == null ? Collections.emptyList() : operatingsystemrelease;
    }
  }
}
