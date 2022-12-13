package com.intellij.dmserver.artifacts;

import com.intellij.dmserver.facet.DMBundleFacet;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.artifacts.ModifiableArtifact;
import com.intellij.packaging.artifacts.ModifiableArtifactModel;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.impl.elements.ArtifactRootElementImpl;
import icons.DmServerSupportIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.dmserver.model.JpsDMBundleArtifactType;

import javax.swing.*;

public final class DMBundleArtifactType extends DMContainerArtifactTypeBase {
  private static final VirtualFileFilter JAR_OR_WAR_FILE_FILTER = new ByExtensionFilter(JpsDMBundleArtifactType.JAR_EXTENSION,
                                                                                        JpsDMBundleArtifactType.WAR_EXTENSION);

  public static DMBundleArtifactType getInstance() {
    return EP_NAME.findExtension(DMBundleArtifactType.class);
  }

  public DMBundleArtifactType() {
    super(JpsDMBundleArtifactType.TYPE_ID, DmServerBundle.messagePointer("DMBundleArtifactType.title"));
  }

  @NotNull
  @Override
  protected VirtualFileFilter getMainFileToDeployFilter(@NotNull Artifact artifact) {
    return JAR_OR_WAR_FILE_FILTER;
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return DmServerSupportIcons.Bundle;
  }

  @NotNull
  @Override
  public CompositePackagingElement<?> createRootElement(@NotNull String artifactName) {
    return new ArtifactRootElementImpl();
  }

  @Override
  public Artifact createArtifactFor(@NotNull Module module, @NotNull DMBundleFacet facet) {
    Project project = module.getProject();
    ArtifactManager manager = ArtifactManager.getInstance(project);
    Artifact artifact = manager
      .addArtifact(DmServerBundle.message("DMBundleArtifactType.artifact.name", module.getName()), getInstance(), null);

    ModifiableArtifactModel modifiableModel = manager.createModifiableModel();
    ModifiableArtifact modifiableArtifact = modifiableModel.getOrCreateModifiableArtifact(artifact);
    addOrFindModuleReference(modifiableArtifact.getRootElement(), module);
    modifiableModel.commit();

    return artifact;
  }

  @Override
  public void synchronizeArtifact(@NotNull ModifiableArtifact artifact, @NotNull Module module, @NotNull DMBundleFacet facet) {
    //nothing
  }
}
