package com.intellij.dmserver.deploy.jmx;

import com.intellij.dmserver.run.DMServerInstance;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URI;


public class ConnectorDeployFromUriCommand extends AbstractDeployCommand {

  @NotNull
  private final URI myUri;

  public ConnectorDeployFromUriCommand(DMServerInstance dmServer, @NotNull URI uri) {
    super(dmServer);
    myUri = uri;
  }

  @Override
  protected String getUri() throws MalformedURLException {
    return myUri.toString();
  }
}
