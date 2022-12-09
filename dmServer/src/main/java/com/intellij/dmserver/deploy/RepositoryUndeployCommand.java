package com.intellij.dmserver.deploy;

import com.intellij.dmserver.run.DMServerInstance;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class RepositoryUndeployCommand implements IDMCommand<Object> {
  private final DMServerInstance myServerInstance;
  private final List<VirtualFile> myFilesToUndeploy;

  public RepositoryUndeployCommand(DMServerInstance serverInstance, List<VirtualFile> filesToUndeploy) {
    myServerInstance = serverInstance;
    myFilesToUndeploy = filesToUndeploy;
  }

  @Override
  public Object execute() throws IOException, TimeoutException, ExecutionException {
    myServerInstance.removeFromRepository(myFilesToUndeploy);
    return new Object();
  }
}
