package com.intellij.dmserver.deploy;

import com.intellij.dmserver.run.DMServerInstance;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class RepositoryDeployCommand implements IDMCommand<Object> {
  private final DMServerInstance myServerInstance;
  private final List<VirtualFile> myFilesToDeploy;

  public RepositoryDeployCommand(DMServerInstance serverInstance, List<VirtualFile> filesToDeploy) {
    myServerInstance = serverInstance;
    myFilesToDeploy = filesToDeploy;
  }

  @Override
  public Object execute() throws IOException, TimeoutException, ExecutionException {
    return myServerInstance.addToRepository(myFilesToDeploy) ? new Object() : null;
  }
}
