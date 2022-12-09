package com.intellij.dmserver.deploy.jmx;

import com.intellij.dmserver.run.DMServerInstance;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;


public abstract class ConnectorFileDeployCommandBase extends AbstractDeployCommand {

  private final VirtualFile myFileToDeploy;

  public ConnectorFileDeployCommandBase(DMServerInstance dmServer, @NotNull VirtualFile fileToDeploy) {
    super(dmServer);
    myFileToDeploy = fileToDeploy;
  }

  @Override
  protected boolean prepareExecution() {
    return getServerInstance().getServerModel().prepareDeploy(myFileToDeploy);
  }

  @Override
  protected String getUri() throws MalformedURLException {
    URL stagingRootURL =
      getServerInstance().getServerModel().computeServerAccessibleStagingURL(getPath(myFileToDeploy));
    URL publishedURL = new URL(stagingRootURL, myFileToDeploy.getName());
    return publishedURL.toString();
  }

  protected abstract String getPath(VirtualFile fileToDeploy);
}
