package com.intellij.dmserver.test;

import com.intellij.dmserver.artifacts.DMParArtifactType;
import com.intellij.dmserver.artifacts.DMPlanArtifactType;
import com.intellij.dmserver.artifacts.WithModuleArtifactUtil;
import com.intellij.dmserver.facet.DMCompositeFacet;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.packaging.artifacts.*;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.impl.elements.ArtifactPackagingElement;
import org.jetbrains.annotations.NotNull;

public class DMArtifactSynchronizationTest extends DMTestBase {
  private static DMCompositeFacet getCompositeFacet(Module module) {
    DMCompositeFacet result =  DMCompositeFacet.getInstance(module);
    assertNotNull(result);
    return result;
  }

  public void testMissingReferencesAddedToPlan() throws Throwable {
    Module[] nestedModules = createNestedModules();

    Module planModule2synchronize = initPlanModule("SynchronizingPlanModule", "synchronizing-plan", nestedModules);

    Module planModule2produceArtifact = initPlanModule("ArtifactProducingModule", "artifact-producing-plan", Module.EMPTY_ARRAY);

    Artifact artifact2synchronize = assertOneElement(WithModuleArtifactUtil.findWithModuleArtifactsFor(planModule2produceArtifact));

    for (Module nestedModule : nestedModules) {
      assertNull("Packaging element for the module is expected not to exist before synchronization: " + nestedModule.getName(),
                 findPackagingElementForModule(artifact2synchronize.getRootElement(), nestedModule));

    }

    synchronizeArtifact(artifact2synchronize, planModule2synchronize);

    for (Module nestedModule : nestedModules) {
      assertNotNull("Packaging element for the module is expected to be added: " + nestedModule.getName(),
                    findPackagingElementForModule(artifact2synchronize.getRootElement(), nestedModule));
    }
  }

  public void testNotNeededReferencesRemovedFromPlan() throws Throwable {
    Module[] nestedModules = createNestedModules();

    Module planModule2synchronize = initPlanModule("SynchronizingPlanModule", "synchronizing-plan", Module.EMPTY_ARRAY);

    Module planModule2produceArtifact = initPlanModule("ArtifactProducingModule", "artifact-producing-plan", nestedModules);

    Artifact artifact2synchronize = assertOneElement(WithModuleArtifactUtil.findWithModuleArtifactsFor(planModule2produceArtifact));

    for (Module nestedModule : nestedModules) {
      assertNotNull("Packaging element for the module is expected to exist before synchronization: " + nestedModule.getName(),
                    findPackagingElementForModule(artifact2synchronize.getRootElement(), nestedModule));
    }

    synchronizeArtifact(artifact2synchronize, planModule2synchronize);

    for (Module nestedModule : nestedModules) {
      assertNull("Packaging element for the module is expected to be removed: " + nestedModule.getName(),
                 findPackagingElementForModule(artifact2synchronize.getRootElement(), nestedModule));
    }
  }

  public void testNeededReferencesPersistInPlan() throws Throwable {
    Module[] nestedModules = createNestedModules();

    Module planModule2synchronize = initPlanModule("SynchronizingPlanModule", "synchronizing-plan", nestedModules);

    Module planModule2produceArtifact = initPlanModule("ArtifactProducingModule", "artifact-producing-plan", nestedModules);

    Artifact artifact2synchronize = assertOneElement(WithModuleArtifactUtil.findWithModuleArtifactsFor(planModule2produceArtifact));

    for (Module nestedModule : nestedModules) {
      assertNotNull("Packaging element for the module is expected to exist before synchronization: " + nestedModule.getName(),
                    findPackagingElementForModule(artifact2synchronize.getRootElement(), nestedModule));
    }

    synchronizeArtifact(artifact2synchronize, planModule2synchronize);

    for (Module nestedModule : nestedModules) {
      assertNotNull("Packaging element for the module is expected to persist: " + nestedModule.getName(),
                    findPackagingElementForModule(artifact2synchronize.getRootElement(), nestedModule));
    }
  }

  public void testIncompatibleArtifactAndModuleNotSynchronized() throws Throwable {
    Module[] nestedModules = createNestedModules();

    Module parModule2synchronize = initParModule("SynchronizingParModule", "synchronizing-par", nestedModules);

    Module planModule2produceArtifact = initPlanModule("ArtifactProducingModule", "artifact-producing-plan", nestedModules);

    Artifact artifact2synchronize = assertOneElement(WithModuleArtifactUtil.findWithModuleArtifactsFor(planModule2produceArtifact));

    try {
      ModifiableArtifactModel modifiableArtifactModel = ArtifactManager.getInstance(myProject).createModifiableModel();
      ModifiableArtifact modifiableArtifact = modifiableArtifactModel.getOrCreateModifiableArtifact(artifact2synchronize);
      DMParArtifactType.getInstance().synchronizeArtifact(modifiableArtifact,
                                                          parModule2synchronize,
                                                          getCompositeFacet(parModule2synchronize));
      fail("IllegalArgumentException is expected to be thrown if the artifact is incompatible");
    }
    catch (IllegalArgumentException ex) {

    }
  }

  private void synchronizeArtifact(@NotNull Artifact artifact, @NotNull Module module) {
    WriteAction.run(() -> {
      ModifiableArtifactModel modifiableArtifactModel = ArtifactManager.getInstance(myProject).createModifiableModel();
      ModifiableArtifact modifiableArtifact = modifiableArtifactModel.getOrCreateModifiableArtifact(artifact);
      DMPlanArtifactType.getInstance().synchronizeArtifact(modifiableArtifact, module, getCompositeFacet(module));
      modifiableArtifactModel.commit();
    });
  }

  private ArtifactPackagingElement findPackagingElementForModule(CompositePackagingElement<?> artifactReferencesContainer, Module module) {
    ArtifactPointerManager pointerManager = ArtifactPointerManager.getInstance(getProject());
    ArtifactManager artifactManager = ArtifactManager.getInstance(getProject());

    for (PackagingElement<?> packagingElement : artifactReferencesContainer.getChildren()) {
      if (!(packagingElement instanceof ArtifactPackagingElement)) {
        continue;
      }
      ArtifactPackagingElement artifactPackagingElement = (ArtifactPackagingElement)packagingElement;
      ArtifactPointer nestedArtifactPointer = pointerManager.createPointer(artifactPackagingElement.getArtifactName());
      Artifact nestedArtifact = nestedArtifactPointer.findArtifact(artifactManager);

      Module nestedArtifactModule = WithModuleArtifactUtil.findModuleFor(getProject(), nestedArtifact);
      if (nestedArtifactModule == module) {
        return artifactPackagingElement;
      }
    }
    return null;
  }

  private Module[] createNestedModules() throws Throwable {
    return new Module[]{
      initBundleModule("NestedBundleModule"),
      initPlanModule("NestedPlanModule", "the-nested-plan", Module.EMPTY_ARRAY),
      initParModule("NestedParModule", "the-nested-par", Module.EMPTY_ARRAY)
    };
  }
}
