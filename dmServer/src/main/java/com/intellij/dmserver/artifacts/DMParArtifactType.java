package com.intellij.dmserver.artifacts;

import com.intellij.dmserver.facet.DMCompositeFacet;
import com.intellij.dmserver.facet.DMCompositeFacetConfiguration;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.artifacts.ModifiableArtifact;
import com.intellij.packaging.artifacts.ModifiableArtifactModel;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElementFactory;
import com.intellij.packaging.impl.elements.ArchivePackagingElement;
import icons.DmServerSupportIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class DMParArtifactType extends DMCompositeArtifactTypeBase {
  @NonNls
  private static final String ARTIFACT_ID_DM_PAR = "dm.par";

  @NonNls
  public static final String PAR_EXTENSION = "par";

  private static final VirtualFileFilter PAR_FILE_FILTER = new ByExtensionFilter(PAR_EXTENSION);

  public static DMParArtifactType getInstance() {
    return EP_NAME.findExtension(DMParArtifactType.class);
  }

  public DMParArtifactType() {
    super(ARTIFACT_ID_DM_PAR, DmServerBundle.messagePointer("DMParArtifactType.display.name"), false);
  }

  @NotNull
  @Override
  protected VirtualFileFilter getMainFileToDeployFilter(@NotNull Artifact artifact) {
    return PAR_FILE_FILTER;
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return DmServerSupportIcons.ParBundle;
  }

  @NotNull
  @Override
  public CompositePackagingElement<?> createRootElement(@NotNull String artifactName) {
    return PackagingElementFactory.getInstance().createArchive("");
  }

  @Override
  public Artifact createArtifactFor(@NotNull Module module, @NotNull DMCompositeFacet facet) {
    Project project = module.getProject();
    ArtifactManager manager = ArtifactManager.getInstance(project);
    Artifact artifact = manager.addArtifact("dm PAR for '" + module.getName() + "'", this, null);

    ModifiableArtifactModel modifiableModel = manager.createModifiableModel();
    ModifiableArtifact modifiableArtifact = modifiableModel.getOrCreateModifiableArtifact(artifact);

    ArchivePackagingElement root = (ArchivePackagingElement)modifiableArtifact.getRootElement();
    root.setArchiveFileName(module.getName() + "." + PAR_EXTENSION);
    DMCompositePackagingElement compositePackaging = addOrFindModuleReference(root, module);
    addNestedBundlesReferences(module, compositePackaging, facet);

    modifiableModel.commit();

    return artifact;
  }

  @Override
  public void synchronizeArtifact(@NotNull ModifiableArtifact artifact, @NotNull Module module, @NotNull DMCompositeFacet facet) {
    if (!isCompatibleArtifact(artifact)) {
      throw new IllegalArgumentException("I am not compatible with artifact: " + artifact);
    }

    DMCompositePackagingElement compositePackaging = addOrFindModuleReference(artifact.getRootElement(), module);
    synchronizeNestedBundlesReferences(module, compositePackaging, facet);
  }

  @Override
  public void updateModuleSupport(@NotNull Module module,
                                  @NotNull DMCompositeFacet facet,
                                  @NotNull ModuleRootModel rootModel,
                                  @NotNull DMCompositeFacetConfiguration facetConfiguration) {
    ManifestManager.getParInstance()
      .createManifest(module, module, rootModel, facetConfiguration.getName(module), facetConfiguration.getVersion());
  }

}
