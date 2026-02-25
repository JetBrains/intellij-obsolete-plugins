// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.intellij.groovy.grails.maven;

import com.intellij.facet.ModifiableFacetModel;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.module.Module;
import com.intellij.util.Consumer;
import com.intellij.util.PairConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.importing.MavenImporter;
import org.jetbrains.idea.maven.importing.MavenRootModelAdapter;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectChanges;
import org.jetbrains.idea.maven.project.MavenProjectsProcessorTask;
import org.jetbrains.idea.maven.project.MavenProjectsTree;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.grails.util.GrailsFacetProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class GrailsMavenImporter extends MavenImporter {
  private static final Set<String> IMPORTED_ARTIFACT_GROUPS = Set.of("javax.servlet", "org.grails");

  GrailsMavenImporter() {
    super("org.grails", "grails-maven-plugin");
  }

  @Override
  public void process(@NotNull IdeModifiableModelsProvider modifiableModelsProvider,
                      @NotNull Module module,
                      @NotNull MavenRootModelAdapter rootModel,
                      @NotNull MavenProjectsTree mavenModel,
                      @NotNull MavenProject mavenProject,
                      @NotNull MavenProjectChanges changes,
                      @NotNull Map<MavenProject, String> mavenProjectToModuleName,
                      @NotNull List<MavenProjectsProcessorTask> postTasks) {

    final List<Consumer<ModifiableFacetModel>> actions = new ArrayList<>();
    for (GrailsFacetProvider provider : GrailsFacetProvider.EP_NAME.getExtensions()) {
      provider.addFacets(actions, module, Collections.singletonList(mavenProject.getDirectoryFile()));
    }

    final ModifiableFacetModel model = modifiableModelsProvider.getModifiableFacetModel(module);
    for (Consumer<ModifiableFacetModel> action : actions) {
      action.consume(model);
    }
  }

  @Override
  public void collectSourceRoots(MavenProject mavenProject, PairConsumer<String, JpsModuleSourceRootType<?>> result) {
    for (Map.Entry<JpsModuleSourceRootType<?>, Collection<String>> entry : GrailsFramework.GRAILS_SOURCE_FOLDERS.entrySet()) {
      JpsModuleSourceRootType<?> type = entry.getKey();

      for (String path : entry.getValue()) {
        result.consume(path, type);
      }
    }
  }

}
