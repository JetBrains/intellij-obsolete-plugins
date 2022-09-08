/*
 * Copyright (c) 2000-2007 JetBrains s.r.o. All Rights Reserved.
 */

package com.intellij.frameworks.jboss.seam;

import com.intellij.facet.FacetTypeId;
import com.intellij.javaee.DeploymentDescriptorsConstants;
import com.intellij.javaee.application.facet.JavaeeApplicationFacet;
import com.intellij.javaee.artifact.JavaeeArtifactUtil;
import com.intellij.javaee.ejb.facet.EjbFacet;
import com.intellij.javaee.ejb.model.xml.converters.EjbRootDescriptor;
import com.intellij.javaee.facet.JavaeeFacet;
import com.intellij.javaee.web.artifact.WebArtifactUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.*;
import com.intellij.packaging.elements.ArtifactRootElement;
import com.intellij.packaging.elements.PackagingElementFactory;
import com.intellij.packaging.impl.elements.ArchivePackagingElement;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.descriptors.ConfigFileMetaData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public final class JavaeeTestUtil {
  private static final Logger LOG = Logger.getInstance(JavaeeTestUtil.class);

  @Nullable
  public static WebFacet getWebFacet(@NotNull Module module) {
    return ContainerUtil.getFirstItem(WebFacet.getInstances(module));
  }

  public static Artifact getWebArtifact(Project project) {
    final Collection<? extends Artifact> collection =
      ArtifactManager.getInstance(project).getArtifactsByType(WebArtifactUtil.getInstance().getExplodedWarArtifactType());
    return ContainerUtil.getFirstItem(collection, null);
  }

  public static Artifact addWebArtifact(Module module, final boolean buildOnMake) {
    WebFacet webFacet = JavaeeTestUtil.getWebFacet(module);
    final ArtifactRootElement<?> rootElement = PackagingElementFactory.getInstance().createArtifactRootElement();
    rootElement.addOrFindChild(JavaeeArtifactUtil.getInstance().createFacetResourcesElement(webFacet));
    final ArtifactType type = WebArtifactUtil.getInstance().getExplodedWarArtifactType();
    final ArtifactManager artifactManager = ArtifactManager.getInstance(module.getProject());
    final Artifact artifact = artifactManager.addArtifact(module.getName() + ":web", type, rootElement);
    if (buildOnMake) {
      final ModifiableArtifactModel model = artifactManager.createModifiableModel();
      model.getOrCreateModifiableArtifact(artifact).setBuildOnMake(true);
      WriteAction.runAndWait(() -> model.commit());
    }
    artifactManager.addElementsToDirectory(artifact, "WEB-INF/classes", PackagingElementFactory.getInstance().createModuleOutput(module));
    return artifact;
  }

  @Nullable
  public static EjbFacet getEjbFacet(@NotNull Module module) {
    return ContainerUtil.getFirstItem(EjbFacet.getInstances(module));
  }

  @Nullable
  public static JavaeeApplicationFacet getJavaeeAppFacet(@NotNull Module module) {
    return ContainerUtil.getFirstItem(JavaeeApplicationFacet.getInstances(module));
  }

  public static ConfigFileMetaData getMainMetaData(FacetTypeId<? extends JavaeeFacet> typeId) {
    if (WebFacet.ID.equals(typeId)) {
      return DeploymentDescriptorsConstants.WEB_XML_META_DATA;
    }
    if (JavaeeApplicationFacet.ID.equals(typeId)) {
      return DeploymentDescriptorsConstants.APPLICATION_XML_META_DATA;
    }
    if (EjbFacet.ID.equals(typeId)) {
      return EjbRootDescriptor.EJB_JAR_META_DATA;
    }
    LOG.error(String.valueOf(typeId));
    return null;
  }

  @Nullable
  public static JavaeeFacet getJavaeeFacet(@NotNull Module module) {
    final WebFacet webFacet = getWebFacet(module);
    if (webFacet != null) {
      return webFacet;
    }
    final EjbFacet ejbFacet = getEjbFacet(module);
    if (ejbFacet != null) {
      return ejbFacet;
    }
    return getJavaeeAppFacet(module);
  }

  public static Artifact createWebArtifact(final Project project, WebFacet... facets) {
    return createWebArtifact(project, false, facets);
  }

  public static Artifact createWebArtifact(final Project project, final boolean archive, WebFacet... facets) {
    final ModifiableArtifactModel model = ArtifactManager.getInstance(project).createModifiableModel();
    final ArtifactType type =
      archive ? WebArtifactUtil.getInstance().getWarArtifactType() : WebArtifactUtil.getInstance().getExplodedWarArtifactType();
    final ModifiableArtifact artifact = model.addArtifact("web", type);
    if (archive) {
      ((ArchivePackagingElement)artifact.getRootElement()).setArchiveFileName("web.war");
    }
    for (WebFacet facet : facets) {
      artifact.getRootElement().addOrFindChild(JavaeeArtifactUtil.getInstance().createFacetResourcesElement(facet));
    }
    WriteCommandAction.writeCommandAction(project).run(() -> model.commit());

    return artifact;
  }
}
