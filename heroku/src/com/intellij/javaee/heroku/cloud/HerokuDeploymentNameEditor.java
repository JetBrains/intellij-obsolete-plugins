package com.intellij.javaee.heroku.cloud;

import com.intellij.remoteServer.util.CloudDeploymentNameEditor;

/**
 * @author michael.golubev
 */
public class HerokuDeploymentNameEditor extends CloudDeploymentNameEditor<HerokuDeploymentConfiguration> {

  public HerokuDeploymentNameEditor() {
    super(() -> new HerokuDeploymentConfiguration(), "Use custom application name:");
  }
}
