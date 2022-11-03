package com.intellij.dmserver.deploy;

import com.intellij.dmserver.artifacts.DMArtifactTypeBase;
import com.intellij.dmserver.artifacts.DMPlanArtifactType;
import com.intellij.dmserver.deploy.jmx.ConnectorDeployCommand;
import com.intellij.dmserver.deploy.jmx.ConnectorForceCheckCommand;
import com.intellij.dmserver.run.DMServerInstance;
import com.intellij.javaee.appServers.deployment.DeploymentModel;
import com.intellij.javaee.ui.packaging.WarArtifactType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

public class DeployCommandsCollector extends CommandsCollector {

  public DeployCommandsCollector(@NotNull DMServerInstance serverInstance, @NotNull DeploymentModel model) {
    super(serverInstance, model);
  }

  @Override
  protected void collectCommandsForDmArtifact(@NotNull Artifact artifact,
                                              @NotNull DMArtifactTypeBase type,
                                              @NotNull List<IDMCommand> result) throws IOException {
    if (type instanceof DMPlanArtifactType) {
      List<VirtualFile> secondaryFilesToDeploy = ((DMPlanArtifactType)type).findSecondaryFilesToDeploy(artifact);
      result.add(new RepositoryDeployCommand(getServerInstance(), secondaryFilesToDeploy));
      result.add(new ConnectorForceCheckCommand(getServerInstance()));
    }

    VirtualFile mainFileToDeploy = type.findMainFileToDeploy(artifact);
    result.add(createConnectorDeployCommand(mainFileToDeploy));
  }

  @Override
  protected void collectCommandsForWarArtifact(@NotNull Artifact artifact,
                                               @NotNull WarArtifactType type,
                                               @NotNull List<IDMCommand> result) throws IOException {
    VirtualFile mainFileToDeploy = DMArtifactTypeBase.findFileByExtension(artifact, "war");
    result.add(createConnectorDeployCommand(mainFileToDeploy));
  }

  private ConnectorDeployCommand createConnectorDeployCommand(@Nullable VirtualFile mainFileToDeploy) throws IOException {
    if (mainFileToDeploy == null) {
      throw new IOException("Can't find main file to deploy");
    }
    return new ConnectorDeployCommand(getServerInstance(), getModel(), mainFileToDeploy);
  }
}
