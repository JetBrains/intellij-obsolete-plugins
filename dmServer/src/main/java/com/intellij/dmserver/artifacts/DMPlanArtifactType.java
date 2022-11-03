package com.intellij.dmserver.artifacts;

import com.intellij.dmserver.artifacts.plan.DMArtifactElementType;
import com.intellij.dmserver.artifacts.plan.PlanArtifactElement;
import com.intellij.dmserver.artifacts.plan.PlanFileManager;
import com.intellij.dmserver.artifacts.plan.PlanRootElement;
import com.intellij.dmserver.facet.*;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.artifacts.ModifiableArtifact;
import com.intellij.packaging.artifacts.ModifiableArtifactModel;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.impl.elements.ArtifactRootElementImpl;
import com.intellij.psi.xml.XmlElement;
import icons.DmServerSupportIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.dmserver.model.JpsDMBundleArtifactType;
import org.osgi.framework.VersionRange;

import javax.swing.*;
import java.util.*;

/**
 * @author michael.golubev
 */
public final class DMPlanArtifactType extends DMCompositeArtifactTypeBase {
  private static final Logger LOG = Logger.getInstance(DMPlanArtifactType.class);

  @NonNls
  private static final String ARTIFACT_ID_DM_PLAN = "dm.plan";

  @NonNls
  private static final String PLAN_EXTENSION = "plan";

  @NonNls
  public static final String PLAN_EXTENSION_WITH_DOT = "." + PLAN_EXTENSION;

  private static final VirtualFileFilter NESTED_FILE_FILTER = new ByExtensionFilter(
    JpsDMBundleArtifactType.JAR_EXTENSION,
    JpsDMBundleArtifactType.WAR_EXTENSION,
    DMParArtifactType.PAR_EXTENSION,
    PLAN_EXTENSION,
    DMConfigArtifactType.PROPERTIES_EXTENSION);

  private static final Map<DMUnitType, DMArtifactElementType> ourUnit2ArtifactElementType;

  static {
    ourUnit2ArtifactElementType = new HashMap<>();
    ourUnit2ArtifactElementType.put(DMUnitType.BUNDLE, DMArtifactElementType.TYPE_BUNDLE);
    ourUnit2ArtifactElementType.put(DMUnitType.PAR, DMArtifactElementType.TYPE_PAR);
    ourUnit2ArtifactElementType.put(DMUnitType.PLAN, DMArtifactElementType.TYPE_PLAN);
    ourUnit2ArtifactElementType.put(DMUnitType.CONFIG, DMArtifactElementType.TYPE_CONFIG);
  }

  public static DMArtifactElementType getElementType4UnitType(DMUnitType unitType) {
    return ourUnit2ArtifactElementType.get(unitType);
  }

  public static DMPlanArtifactType getInstance() {
    return EP_NAME.findExtension(DMPlanArtifactType.class);
  }

  public DMPlanArtifactType() {
    super(ARTIFACT_ID_DM_PLAN, DmServerBundle.messagePointer("DMPlanArtifactType.presentable.name"), true);
  }

  @NotNull
  @Override
  protected VirtualFileFilter getMainFileToDeployFilter(@NotNull Artifact artifact) {
    DMCompositePackagingElement moduleReference = findModuleReference(artifact);
    Module module = moduleReference.findModule();
    DMCompositeFacet facet = DMCompositeFacet.getInstance(module);
    final String planFileName = getPlanFileName(facet);
    return file -> planFileName.equals(file.getName());
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return DmServerSupportIcons.DM;
  }

  @NotNull
  @Override
  public CompositePackagingElement<?> createRootElement(@NotNull String s) {
    return new ArtifactRootElementImpl();
  }

  @Override
  public Artifact createArtifactFor(@NotNull Module module, @NotNull DMCompositeFacet facet) {
    Project project = module.getProject();
    ArtifactManager manager = ArtifactManager.getInstance(project);
    Artifact artifact = manager.addArtifact("dm Plan for '" + module.getName() + "'", this, null);

    ModifiableArtifactModel modifiableModel = manager.createModifiableModel();
    ModifiableArtifact modifiableArtifact = modifiableModel.getOrCreateModifiableArtifact(artifact);
    addOrFindModuleReference(modifiableArtifact.getRootElement(), module);

    addNestedBundlesReferences(module, modifiableArtifact.getRootElement(), facet);

    modifiableModel.commit();

    return artifact;
  }

  @Override
  public void synchronizeArtifact(@NotNull ModifiableArtifact artifact, @NotNull Module module, @NotNull DMCompositeFacet facet) {
    if (!isCompatibleArtifact(artifact)) {
      throw new IllegalArgumentException("I am not compatible with artifact: " + artifact);
    }

    synchronizeNestedBundlesReferences(module, artifact.getRootElement(), facet);
  }

  private DMCompositePackagingElement findModuleReference(Artifact artifact) {
    for (PackagingElement element : artifact.getRootElement().getChildren()) {
      if (element instanceof DMCompositePackagingElement) {
        return (DMCompositePackagingElement)element;
      }
    }
    return null;
  }

  public List<VirtualFile> findSecondaryFilesToDeploy(Artifact artifact) {
    VirtualFileFilter mainFileToDeployFilter = getMainFileToDeployFilter(artifact);
    List<VirtualFile> result = new ArrayList<>();
    VirtualFile output = LocalFileSystem.getInstance().findFileByPath(artifact.getOutputPath());
    for (VirtualFile nextToDeploy : output.getChildren()) {
      if (NESTED_FILE_FILTER.accept(nextToDeploy) && !mainFileToDeployFilter.accept(nextToDeploy)) {
        result.add(nextToDeploy);
      }
    }
    return result;
  }

  @Override
  public void updateModuleSupport(@NotNull final Module module,
                                  @NotNull final DMCompositeFacet facet,
                                  @NotNull final ModuleRootModel rootModel,
                                  @NotNull final DMCompositeFacetConfiguration facetConfiguration) {
    Runnable saveRunnable = () -> WriteAction.run(() -> doUpdateModuleSupport(module, facet, rootModel, facetConfiguration));
    StartupManager.getInstance(module.getProject()).runWhenProjectIsInitialized(saveRunnable);
  }

  private void doUpdateModuleSupport(@NotNull Module module,
                                     @NotNull DMCompositeFacet facet,
                                     @NotNull ModuleRootModel rootModel,
                                     @NotNull DMCompositeFacetConfiguration facetConfiguration) {

    PlanFileManager planManager = facet.getPlanFileManager();

    PlanFileManager.PlanFileCreator planCreator = planManager.createPlan(rootModel);
    PlanRootElement rootElement = planCreator.getRootElement();

    if (!LOG.assertTrue(rootElement != null)) {
      return;
    }

    rootElement.getName().setValue(facetConfiguration.getName(module));
    rootElement.getVersion().setValue(facetConfiguration.getVersion());
    rootElement.getScoped().setValue(facetConfiguration.getScoped());
    rootElement.getAtomic().setValue(facetConfiguration.getAtomic());

    List<NestedUnitIdentity> newUnitIdentities = new ArrayList<>();
    List<DMUnitDescriptor> newUnitDescriptors = new ArrayList<>();

    for (NestedUnitIdentity unitIdentity : facetConfiguration.getNestedBundles()) {
      Module artifactModule = unitIdentity.getModule();
      if (artifactModule == null) {
        continue;
      }

      DMUnitDescriptor unitDescriptor = DMUnitDescriptorProvider.getInstance().processModule(artifactModule);
      if (unitDescriptor == null) {
        continue;
      }

      newUnitIdentities.add(unitIdentity);
      newUnitDescriptors.add(unitDescriptor);
    }

    Iterator<DMUnitDescriptor> itNewUnitDescriptor = newUnitDescriptors.iterator();

    DMUnitDescriptor nextNewUnitDescriptor = itNewUnitDescriptor.hasNext() ? itNewUnitDescriptor.next() : null;

    Map<String, PlanArtifactElement> name2newElements = new HashMap<>();

    List<PlanArtifactElement> elements2undefine = new ArrayList<>();

    List<PlanArtifactElement> oldArtifactElements = rootElement.getArtifacts();
    for (PlanArtifactElement oldArtifactElement : oldArtifactElements.toArray(new PlanArtifactElement[0])) {
      if (nextNewUnitDescriptor != null) {
        String oldArtifactElementName = oldArtifactElement.getName().getValue();
        name2newElements.put(oldArtifactElementName, oldArtifactElement);
        if (nextNewUnitDescriptor.getSymbolicName().equals(oldArtifactElementName)) {
          // element is in place
          nextNewUnitDescriptor = itNewUnitDescriptor.hasNext() ? itNewUnitDescriptor.next() : null;
        }
        else {
          // element either moved or deleted
          elements2undefine.add(oldArtifactElement);
        }
      }
      else {
        // element is deleted
        elements2undefine.add(oldArtifactElement);
      }
    }

    while (nextNewUnitDescriptor != null) {
      // element is added or moved
      PlanArtifactElement newArtifactElement = rootElement.addArtifact();
      String newElementName = nextNewUnitDescriptor.getSymbolicName();
      PlanArtifactElement movedArtifactElement = name2newElements.get(newElementName);
      if (movedArtifactElement != null) {
        XmlElement newXmlElement = newArtifactElement.getXmlElement();
        XmlElement movedXmlElement = movedArtifactElement.getXmlElement();
        if (LOG.assertTrue(newXmlElement != null)
            && LOG.assertTrue(movedXmlElement != null)) {
          newXmlElement.replace(movedXmlElement.copy());
        }
      }
      name2newElements.put(newElementName, newArtifactElement);
      nextNewUnitDescriptor = itNewUnitDescriptor.hasNext() ? itNewUnitDescriptor.next() : null;
    }

    for (PlanArtifactElement element2undefine : elements2undefine) {
      element2undefine.undefine();
    }

    itNewUnitDescriptor = newUnitDescriptors.iterator();
    Iterator<NestedUnitIdentity> itNewUnitIdentity = newUnitIdentities.iterator();

    for (PlanArtifactElement newArtifactElement : rootElement.getArtifacts()) {
      DMUnitDescriptor newUnitDescriptor = itNewUnitDescriptor.next();
      NestedUnitIdentity newNestedUnitIdentity = itNewUnitIdentity.next();
      newArtifactElement.getType().setValue(getElementType4UnitType(newUnitDescriptor.getType()));
      newArtifactElement.getName().setValue(newUnitDescriptor.getSymbolicName());
      newArtifactElement.getVersion().setValue(new VersionRange(newNestedUnitIdentity.getVersionRange()));
    }

    planCreator.save();
  }

  private static String getPlanFileName(DMCompositeFacet facet) {
    String planName = facet.getConfigurationImpl().getName(facet.getModule());
    return planName.endsWith(PLAN_EXTENSION_WITH_DOT) ? planName : planName + PLAN_EXTENSION_WITH_DOT;
  }
}
