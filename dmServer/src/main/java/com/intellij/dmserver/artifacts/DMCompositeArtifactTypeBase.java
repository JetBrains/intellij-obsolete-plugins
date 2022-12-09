package com.intellij.dmserver.artifacts;

import com.intellij.dmserver.facet.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactPointer;
import com.intellij.packaging.artifacts.ArtifactPointerManager;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.impl.elements.ArtifactPackagingElement;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

public abstract class DMCompositeArtifactTypeBase
  extends DMArtifactTypeBase<DMCompositePackagingElement, DMCompositePackagingElementType, DMCompositeFacet> {

  private final boolean myNestedCompositesAllowed;

  public DMCompositeArtifactTypeBase(@NonNls String id, Supplier<@Nls String> title, boolean nestedCompositesAllowed) {
    super(id, title);
    myNestedCompositesAllowed = nestedCompositesAllowed;
  }

  @Override
  public DMCompositePackagingElementType getModulePackagingElementType() {
    return DMCompositePackagingElementType.getInstance();
  }

  protected final void addNestedBundlesReferences(@NotNull Module module,
                                                  @NotNull CompositePackagingElement<?> destinationElement,
                                                  @NotNull DMCompositeFacet facet) {
    addNestedBundlesReferences(module, destinationElement, facet.getConfigurationImpl().getNestedBundles());
  }

  private void addNestedBundlesReferences(@NotNull Module module,
                                          final @NotNull CompositePackagingElement<?> referencesContainerElement,
                                          @NotNull Collection<NestedUnitIdentity> nestedUnitIdentities) {
    final Project project = module.getProject();
    final ArtifactPointerManager artifactPointerManager = ArtifactPointerManager.getInstance(project);
    new NestedUnitIdentitiesProcessor() {
      @Override
      protected void doProcessIdentity(NestedUnitIdentity nestedUnitIdentity, DMFacetBase nestedUnitFacet) {
        Artifact nextReferenceArtifact = nestedUnitFacet.getMainArtifact();
        if (nextReferenceArtifact == null) {
          return;
        }

        ArtifactPointer nextArtifactPointer = artifactPointerManager.createPointer(nextReferenceArtifact);
        ArtifactPackagingElement nextInclusion = new ArtifactPackagingElement(project, nextArtifactPointer);
        referencesContainerElement.addOrFindChild(nextInclusion);
      }
    }.processIdentities(module, nestedUnitIdentities);
  }

  protected final void synchronizeNestedBundlesReferences(@NotNull Module module,
                                                          @NotNull CompositePackagingElement<?> referencesContainerElement,
                                                          DMCompositeFacet facet) {
    List<ArtifactPackagingElement> toRemove = new LinkedList<>();
    for (PackagingElement<?> nextChild : referencesContainerElement.getChildren()) {
      if (nextChild instanceof ArtifactPackagingElement) {
        toRemove.add((ArtifactPackagingElement)nextChild);
      }
    }
    referencesContainerElement.removeChildren(toRemove);
    addNestedBundlesReferences(module, referencesContainerElement, facet);
  }

  public abstract void updateModuleSupport(@NotNull Module module,
                                           @NotNull DMCompositeFacet facet,
                                           @NotNull ModuleRootModel rootModel,
                                           @NotNull DMCompositeFacetConfiguration facetConfiguration);

  private abstract class NestedUnitIdentitiesProcessor {

    public void processIdentities(@NotNull Module module, Collection<NestedUnitIdentity> nestedUnitIdentities) {
      for (NestedUnitIdentity unitIdentity : nestedUnitIdentities) {
        Module nextNestedModule = unitIdentity.getModule();
        if (nextNestedModule == null || nextNestedModule == module) {
          continue;
        }

        DMFacetBase nextReferencedFacet = DMFacetFinder.getInstance().processModule(nextNestedModule);
        // add osmorc facet specifics here if needed
        if (nextReferencedFacet == null) {
          continue;
        }
        if (!myNestedCompositesAllowed && nextReferencedFacet instanceof DMCompositeFacet) {
          continue;
        }

        doProcessIdentity(unitIdentity, nextReferencedFacet);
      }
    }

    protected abstract void doProcessIdentity(NestedUnitIdentity nestedUnitIdentity, DMFacetBase nestedUnitFacet);
  }
}
