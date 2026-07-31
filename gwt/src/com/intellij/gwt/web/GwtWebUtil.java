package com.intellij.gwt.web;

import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeRegistry;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.javaee.DeploymentDescriptorsConstants;
import com.intellij.javaee.web.WebRoot;
import com.intellij.javaee.web.WebUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.javaee.web.facet.WebFacetConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.descriptors.ConfigFileFactory;
import com.intellij.util.descriptors.ConfigFileMetaData;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GwtWebUtil {
  private GwtWebUtil() {
  }

  public static WebFacet createWebFacet(@NotNull GwtFacet facet, @NotNull VirtualFile root) {
    Project project = facet.getModule().getProject();
    final ConfigFileMetaData webXml = DeploymentDescriptorsConstants.WEB_XML_META_DATA;
    final @NonNls String webRootUrl = root.getUrl() + "/war";
    final String webXmlUrl = webRootUrl + "/" + webXml.getDirectoryPath() + "/" + webXml.getFileName();
    final FacetType<WebFacet, WebFacetConfiguration> webFacetType = FacetTypeRegistry.getInstance().findFacetType(WebFacet.ID);
    final WebFacet webFacet = FacetManager.getInstance(facet.getModule()).addFacet(webFacetType, webFacetType.getDefaultFacetName(), null);
    webFacet.addWebRoot(webRootUrl, "/");
    ConfigFileFactory.getInstance().createFile(project, webXmlUrl, webXml.getDefaultVersion(), false);
    webFacet.getDescriptorsContainer().getConfiguration().replaceConfigFile(webXml, webXmlUrl);
    facet.getConfiguration().setWebFacetName(webFacet.getName());
    return webFacet;
  }

  public static @NotNull @NlsSafe String getOutputPath(@NotNull GwtModule gwtModule, @NotNull String relativePath) {
    return gwtModule.getOutputName() + "/" + relativePath;
  }

  public static @Nullable @NlsSafe String getRelativeToWebRootPath(@NotNull VirtualFile htmlFile, @NotNull Project project) {
    final WebFacet webFacet = WebUtil.findFacetByFileUnderWebRoot(htmlFile, project);
    if (webFacet != null) {
      final WebRoot webRoot = WebUtil.findParentWebRoot(htmlFile, webFacet.getWebRoots());
      if (webRoot != null) {
        final VirtualFile rootFile = webRoot.getFile();
        if (rootFile != null) {
          return VfsUtilCore.getRelativePath(htmlFile, rootFile, '/');
        }
      }
    }
    return null;
  }
}
