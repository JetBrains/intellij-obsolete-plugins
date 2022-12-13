package com.intellij.dmserver.deploy.jmx;

import com.intellij.dmserver.run.DMServerInstance;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;


public class ConnectorFileDeployCommand extends ConnectorFileDeployCommandBase {

  public ConnectorFileDeployCommand(DMServerInstance dmServer, @NotNull VirtualFile fileToDeploy) {
    super(dmServer, fileToDeploy);
  }

  @Override
  protected String getPath(VirtualFile fileToDeploy) {
    return fileToDeploy.getParent().getPath();
  }
}
