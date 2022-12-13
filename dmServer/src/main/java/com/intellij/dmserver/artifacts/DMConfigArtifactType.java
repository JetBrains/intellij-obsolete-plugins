package com.intellij.dmserver.artifacts;

import com.intellij.dmserver.facet.DMConfigFacet;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.artifacts.ModifiableArtifact;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.impl.elements.ArtifactRootElementImpl;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class DMConfigArtifactType extends DMArtifactTypeBase<DMConfigPackagingElement, DMConfigPackagingElementType, DMConfigFacet> {
  @NonNls
  private static final String ARTIFACT_ID_DM_CONFIG = "dm.config";

  @NonNls
  public static final String PROPERTIES_EXTENSION = "properties";

  public static final String DOT_PROPERTIES_EXTENSION = "." + PROPERTIES_EXTENSION;

  private static final VirtualFileFilter PROPERTIES_FILE_FILTER = new ByExtensionFilter(PROPERTIES_EXTENSION);

  public static DMConfigArtifactType getInstance() {
    return EP_NAME.findExtension(DMConfigArtifactType.class);
  }

  public DMConfigArtifactType() {
    super(ARTIFACT_ID_DM_CONFIG, DmServerBundle.messagePointer("DMConfigArtifactType.presentable.name"));
  }

  @Override
  public DMConfigPackagingElementType getModulePackagingElementType() {
    return DMConfigPackagingElementType.getInstance();
  }

  @NotNull
  @Override
  public CompositePackagingElement<?> createRootElement(@NotNull String artifactName) {
    return new ArtifactRootElementImpl();
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return AllIcons.FileTypes.Text;
  }

  @Override
  public Artifact createArtifactFor(@NotNull Module module, @NotNull DMConfigFacet facet) {
    Project project = module.getProject();
    ArtifactManager manager = ArtifactManager.getInstance(project);
    Artifact artifact = manager
      .addArtifact(DmServerBundle.message("DMConfigArtifactType.artifact.name", module.getName()), getInstance(),
                   null);
    addOrFindModuleReference(artifact.getRootElement(), module);
    return artifact;
  }

  @Override
  public void synchronizeArtifact(@NotNull ModifiableArtifact artifact, @NotNull Module module, @NotNull DMConfigFacet facet) {
    //nothing
  }

  @NotNull
  @Override
  protected VirtualFileFilter getMainFileToDeployFilter(@NotNull Artifact artifact) {
    return PROPERTIES_FILE_FILTER;
  }
}
