// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.structure.GrailsApplicationProvider;
import org.jetbrains.plugins.grails.structure.impl.Grails2Application;

public class TestGrailsApplicationProvider extends GrailsApplicationProvider {

  @Nullable
  @Override
  public GrailsApplication createApplication(@NotNull Project project, @NotNull VirtualFile root) {
    final Module module = ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(root);
    if (module == null) return null;
    if (GrailsFramework.getInstance().isAuxModule(module)) return null;
    if (VfsUtil.findRelativeFile(root, "application.properties") == null) return null;
    if (VfsUtil.findRelativeFile(root, "plugin.xml") != null) return null;
    return new Grails2Application(root, module);
  }
}
