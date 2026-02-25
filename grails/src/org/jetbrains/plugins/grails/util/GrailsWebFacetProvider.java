// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.grails.util;

import com.intellij.facet.ModifiableFacetModel;
import com.intellij.javaee.web.WebRoot;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.javaee.web.facet.WebFacetConfiguration;
import com.intellij.javaee.web.facet.WebFacetType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.plugins.grails.config.GrailsStructure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class GrailsWebFacetProvider implements GrailsFacetProvider {
  private static final String GRAILS_WEB_FACET = "GrailsWeb";

  @Override
  public void addFacets(Collection<Consumer<ModifiableFacetModel>> actions,
                        final Module module,
                        Collection<VirtualFile> roots) {
    if (GrailsStructure.isVersionAtLeast("3.0", module)) return;

    final List<VirtualFile> webRoots = new ArrayList<>();
    for (VirtualFile root : roots) {
      ContainerUtil.addIfNotNull(webRoots, root.findChild("web-app"));
      ContainerUtil.addIfNotNull(webRoots, VfsUtil.findRelativeFile(root, "grails-app", "views"));
    }

    actions.add(model -> {
      WebFacet grailsFacet = null;
      Collection<WebFacet> facets = model.getFacetsByType(WebFacet.ID);

      for (WebFacet facet : facets) {
        if (GRAILS_WEB_FACET.equals(facet.getName())) {
          grailsFacet = facet;
        }
        else {
          for (WebRoot webRoot : facet.getWebRoots()) {
            webRoots.remove(webRoot.getFile());
          }
        }
      }

      if (grailsFacet == null) {
        if (!webRoots.isEmpty()) {

          final WebFacetConfiguration configuration = WebFacetType.getInstance().createDefaultConfiguration();
          grailsFacet = WebFacetType.getInstance().createFacet(module, GRAILS_WEB_FACET, configuration, null);

          var facet = grailsFacet;
          for (VirtualFile virtualFile : webRoots) {
            facet.addWebRoot(virtualFile, "/");
          }

          model.addFacet(grailsFacet);
        }
        return;
      }

      final List<WebRoot> toRemove = new ArrayList<>();

      for (WebRoot webRoot : grailsFacet.getWebRoots()) {
        VirtualFile file = webRoot.getFile();
        if (!webRoots.remove(file)) {
          if (file == null || !isInProject(file, grailsFacet.getModule().getProject())) {
            toRemove.add(webRoot);
          }
        }
      }

      if (!toRemove.isEmpty() || !webRoots.isEmpty()) {
        for (VirtualFile virtualFile : webRoots) {
          grailsFacet.addWebRoot(virtualFile, "/");
        }
        for (WebRoot webRoot : toRemove) {
          grailsFacet.removeWebRoot(webRoot);
        }
      }
    });
  }

  public static boolean isInProject(VirtualFile file, Project project) {
    return ProjectRootManager.getInstance(project).getFileIndex().isInContent(file);
  }
}
