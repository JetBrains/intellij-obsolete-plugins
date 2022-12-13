package com.intellij.dmserver.artifacts;

import com.intellij.ProjectTopics;
import com.intellij.dmserver.facet.DMFacetBase;
import com.intellij.dmserver.facet.DMFacetFinder;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ModuleListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.artifacts.ModifiableArtifactModel;
import com.intellij.util.messages.SimpleMessageBusConnection;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author michael.golubev
 */
public final class ArtifactSynchronizer implements ModuleRootListener, ModuleListener {
  private ModifiableArtifactModel myArtifactRemovingModel = null;

  public ArtifactSynchronizer(Project project) {
    SimpleMessageBusConnection connection = project.getMessageBus().simpleConnect();
    connection.subscribe(ProjectTopics.MODULES, this);
    connection.subscribe(ProjectTopics.PROJECT_ROOTS, this);
  }

  @Override
  public void beforeModuleRemoved(@NotNull Project project, @NotNull Module module) {
    DMFacetBase facet = DMFacetFinder.getInstance().processModule(module);
    if (facet != null) {
      List<Artifact> artifactsForModule = WithModuleArtifactUtil.findWithModuleArtifactsFor(module);
      if (!artifactsForModule.isEmpty()) {
        ArtifactManager artifactManager = ArtifactManager.getInstance(project);
        if (myArtifactRemovingModel == null) {
          myArtifactRemovingModel = artifactManager.createModifiableModel();
        }
        for (Artifact nextToRemove : artifactsForModule) {
          myArtifactRemovingModel.removeArtifact(nextToRemove);
        }
      }
    }
  }

  @Override
  public void rootsChanged(@NotNull ModuleRootEvent event) {
    if (myArtifactRemovingModel != null) {
      WriteAction.run(() -> {
        myArtifactRemovingModel.commit();
        myArtifactRemovingModel.dispose();
      });
      myArtifactRemovingModel = null;
    }
  }
}
