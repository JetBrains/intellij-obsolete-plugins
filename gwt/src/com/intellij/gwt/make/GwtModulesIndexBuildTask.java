package com.intellij.gwt.make;

import com.intellij.compiler.server.BuildManager;
import com.intellij.facet.ProjectFacetManager;
import com.intellij.gwt.facet.GwtFacetType;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileTask;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gwt.index.impl.JpsGwtModulesCache;

import java.nio.file.Path;
import java.util.Collection;

final class GwtModulesIndexBuildTask implements CompileTask {
  private static final Logger LOG = Logger.getInstance(GwtModulesIndexBuildTask.class);

  @Override
  public boolean execute(@NotNull CompileContext context) {
    ReadAction.runBlocking(() -> doExecute(context));
    return true;
  }

  private static void doExecute(@NotNull CompileContext context) {
    Project project = context.getProject();
    if (ProjectFacetManager.getInstance(project).hasFacets(GwtFacetType.ID)) {
      GwtBuilderMessageHandler.getInstance(project).installExternalGwtBuilderListener();
      Path projectSystemDirectory = BuildManager.getInstance().getProjectSystemDirectory(project).toPath();
      JpsGwtModulesCache cache = new JpsGwtModulesCache(projectSystemDirectory);
      if (DumbService.isDumb(project)) {
        LOG.debug("Indices aren't available, clearing GWT modules cache");
        cache.clear();
      }
      else {
        Collection<VirtualFile> files = GwtModulesManager.getInstance(project).getGwtModuleFiles(GlobalSearchScope.projectScope(project));
        LOG.debug("Creating GWT modules cache for " + files.size() + " *.gwt.xml files");
        JpsGwtModulesCache.GwtModulesConfiguration modulesConfiguration = new JpsGwtModulesCache.GwtModulesConfiguration();
        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        for (VirtualFile file : files) {
          VirtualFile sourceRoot = fileIndex.getSourceRootForFile(file);
          Module module = fileIndex.getModuleForFile(file);
          if (sourceRoot != null && module != null) {
            boolean inTests = fileIndex.isInTestSourceContent(file);
            modulesConfiguration.myGwtModules.add(new JpsGwtModulesCache.GwtModuleData(file.getPath(), sourceRoot.getPath(), inTests,
                                                                                       module.getName()));
          }
        }
        cache.saveGwtModules(modulesConfiguration);
      }
    }
  }
}
