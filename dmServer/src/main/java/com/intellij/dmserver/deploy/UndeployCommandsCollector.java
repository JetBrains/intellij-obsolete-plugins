package com.intellij.dmserver.deploy;

import com.intellij.dmserver.artifacts.DMArtifactTypeBase;
import com.intellij.dmserver.artifacts.DMPlanArtifactType;
import com.intellij.dmserver.deploy.jmx.ConnectorUndeployCommand;
import com.intellij.dmserver.run.DMServerInstance;
import com.intellij.javaee.appServers.deployment.DeploymentModel;
import com.intellij.javaee.ui.packaging.WarArtifactType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UndeployCommandsCollector extends CommandsCollector {

  public UndeployCommandsCollector(@NotNull DMServerInstance serverInstance, @NotNull DeploymentModel model) {
    super(serverInstance, model);
  }

  @Override
  protected void collectCommandsForDmArtifact(@NotNull Artifact artifact,
                                              @NotNull DMArtifactTypeBase type,
                                              @NotNull List<IDMCommand> result) {
    result.add(createUndeployCommand());

    if (type instanceof DMPlanArtifactType) {
      List<VirtualFile> secondaryFilesToUndeploy = ((DMPlanArtifactType)type).findSecondaryFilesToDeploy(artifact);
      result.add(new RepositoryUndeployCommand(getServerInstance(), secondaryFilesToUndeploy));
    }
  }

  @Override
  protected void collectCommandsForWarArtifact(@NotNull Artifact artifact,
                                               @NotNull WarArtifactType type,
                                               @NotNull List<IDMCommand> result) {
    result.add(createUndeployCommand());
  }

  private ConnectorUndeployCommand createUndeployCommand() {
    return new ConnectorUndeployCommand(getServerInstance(), getModel());
  }
}
