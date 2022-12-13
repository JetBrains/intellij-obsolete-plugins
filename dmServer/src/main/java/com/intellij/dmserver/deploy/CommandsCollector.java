package com.intellij.dmserver.deploy;

import com.intellij.dmserver.artifacts.DMArtifactTypeBase;
import com.intellij.dmserver.run.DMServerInstance;
import com.intellij.javaee.appServers.deployment.DeploymentModel;
import com.intellij.javaee.ui.packaging.WarArtifactType;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactPointer;
import com.intellij.packaging.artifacts.ArtifactType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class CommandsCollector {

  private final DMServerInstance myServerInstance;
  private final DeploymentModel myModel;

  public CommandsCollector(@NotNull DMServerInstance serverInstance, @NotNull DeploymentModel model) {
    myServerInstance = serverInstance;
    myModel = model;
  }

  public List<IDMCommand> collectCommands() throws IOException {
    Artifact artifact = getModel().getArtifact();
    if (artifact == null) {
      final ArtifactPointer artifactPointer = getModel().getArtifactPointer();
      throw new IOException("Deployment fails to access artifact " + (artifactPointer == null ? "" : artifactPointer.getArtifactName()));
    }

    List<IDMCommand> result = new ArrayList<>();
    ArtifactType type = artifact.getArtifactType();
    if (type instanceof DMArtifactTypeBase) {
      collectCommandsForDmArtifact(artifact, (DMArtifactTypeBase)type, result);
    }
    else if (type instanceof WarArtifactType) {
      collectCommandsForWarArtifact(artifact, (WarArtifactType)type, result);
    }
    else {
      throw new IOException("Unexpected artifact type: " + type.getPresentableName() + " found");
    }
    return result;
  }

  protected final DMServerInstance getServerInstance() {
    return myServerInstance;
  }

  protected final DeploymentModel getModel() {
    return myModel;
  }

  protected abstract void collectCommandsForDmArtifact(@NotNull Artifact artifact,
                                                       @NotNull DMArtifactTypeBase type,
                                                       @NotNull List<IDMCommand> result) throws IOException;

  protected abstract void collectCommandsForWarArtifact(@NotNull Artifact artifact,
                                                        @NotNull WarArtifactType type,
                                                        @NotNull List<IDMCommand> result) throws IOException;
}
