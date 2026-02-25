// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.intellij.groovy.grails.maven;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.structure.GrailsApplicationProvider;

final class GrailsMavenApplicationProvider extends GrailsApplicationProvider {

  @Override
  public @Nullable GrailsApplication createApplication(@NotNull Project project, @NotNull VirtualFile root) {
    final Module module = ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(root);
    if (module == null) return null;
    final MavenProject mavenProject = MavenProjectsManager.getInstance(project).findProject(module);
    if (mavenProject == null
        || !mavenProject.getDirectoryFile().equals(root)
        || mavenProject.findPlugin("org.grails", "grails-maven-plugin") == null) {
      return null;
    }
    return new GrailsMavenApplication(module, root, mavenProject.getMavenId());
  }
}
