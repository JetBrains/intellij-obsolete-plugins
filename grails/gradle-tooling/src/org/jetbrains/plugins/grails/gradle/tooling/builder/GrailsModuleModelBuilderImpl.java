// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.gradle.tooling.builder;

import com.intellij.gradle.toolingExtension.util.GradleVersionUtil;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.tooling.Message;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderContext;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Vladislav.Soroka
 */
@SuppressWarnings("SSBasedInspection")
public class GrailsModuleModelBuilderImpl implements ModelBuilderService {
  @Override
  public boolean canBuild(String modelName) {
    return GrailsModule.class.getName().equals(modelName);
  }

  @Override
  public Object buildAll(String modelName, Project project) {
    Context context = Context.from(project);
    if (context == null) return null;

    GrailsVersionInfo grailsVersionInfo = context.myGrailsVersionInfo;
    DefaultExternalModuleDependency dependency = new DefaultExternalModuleDependency(grailsVersionInfo.gradleDependencyGroup, grailsVersionInfo.shellArtifactId, null);
    Configuration configuration = getConfiguration(project);
    configuration.getDependencies().add(dependency);

    String version = (String) project.getProperties().get("grailsVersion");
    if (version == null || version.isEmpty()) {
      version = configuration.getResolvedConfiguration().getFirstLevelModuleDependencies()
        .stream()
        .filter(dep -> grailsVersionInfo.gradleDependencyGroup.equals(dep.getModuleGroup())
                       && grailsVersionInfo.shellArtifactId.equals(dep.getModuleName()))
        .findFirst()
        .map(dep -> dep.getModuleVersion())
        .orElse(null);
    }

    List<String> paths = configuration.resolve().stream().map(file -> file.getAbsolutePath()).collect(Collectors.toList());
    return (version != null && !version.isEmpty()) ? new GrailsModuleImpl(version, context.grailsPluginCoordinates, paths) : null;
  }

  private static Configuration getConfiguration(Project project) {
    if (GradleVersionUtil.isCurrentGradleNewerThan("7.0")) {
      Configuration configuration = project.getConfigurations().getByName("implementation").copy(dependency -> false);
      configuration.setCanBeResolved(true);
      return configuration;
    } else {
      return project.getConfigurations().getByName("compile").copy(dependency -> false);
    }
  }

  @Override
  public void reportErrorMessage(
    @NotNull String modelName,
    @NotNull Project project,
    @NotNull ModelBuilderContext context,
    @NotNull Exception exception
  ) {
    context.getMessageReporter().createMessage()
      .withGroup(this)
      .withKind(Message.Kind.WARNING)
      .withTitle("Grails import errors")
      .withText("Unable to build Grails project configuration")
      .withException(exception)
      .reportMessage(project);
  }

  private static class Context {
    /**
     * Array of Grails gradle plugins (see <a href="https://grails.github.io/grails-doc/latest/guide/single.html#gradlePlugins">Grails plugins for Gradle</a>).
     * If any is present, we make assumption that this is Grails project.
     *
     */
    private static final String[] GRAILS_PLUGIN_NAME_ARRAY = {
      "grails-core",
      "grails-plugin",
      "grails-web",
      "grails-gsp",
      "grails-doc",
    };


    private final @NotNull GrailsModuleModelBuilderImpl.GrailsVersionInfo myGrailsVersionInfo;
    private final @NotNull String grailsPluginCoordinates;


    private Context(@NotNull GrailsModuleModelBuilderImpl.GrailsVersionInfo version, @NotNull String plugin) {
      myGrailsVersionInfo = version;
      grailsPluginCoordinates = plugin;
    }

    private static @Nullable Context from(@NotNull Project project) {
      for (GrailsVersionInfo version : GrailsVersionInfo.values()) {
        for (String pluginName : GRAILS_PLUGIN_NAME_ARRAY) {
          String pluginCoordinates = version.getPluginCoordinates(pluginName);
          if (project.getPlugins().hasPlugin(pluginCoordinates)) {
            return new Context(version, pluginCoordinates);
          }
        }
      }
      return null;
    }
  }

  /**
   * Cordinates parts of Grails may differ from version to version. This enum stores the possible options.
   */
  private enum GrailsVersionInfo {
    GRAILS_3("org.grails", "org.grails", "grails-shell"),
    GRAILS_7("org.apache.grails.gradle", "org.apache.grails", "grails-shell-cli");

    private final String gradlePluginGroup;
    private final String gradleDependencyGroup;
    private final String shellArtifactId;

    GrailsVersionInfo(String gradlePluginGroup, String gradleDependencyGroup, String shellArtifactId) {
      this.gradlePluginGroup = gradlePluginGroup;
      this.gradleDependencyGroup = gradleDependencyGroup;
      this.shellArtifactId = shellArtifactId;
    }

    private String getPluginCoordinates(String pluginName) {
      return gradlePluginGroup + "." + pluginName;
    }
  }
}
