package com.intellij.javaee.heroku.cloud;

import com.intellij.remoteServer.ServerType;
import com.intellij.remoteServer.util.CloudDeploymentNameConfiguration;
import com.intellij.remoteServer.util.CloudGitDeploymentDetector;

import java.util.regex.Pattern;

/**
 * @author michael.golubev
 */
public class HerokuDeploymentDetector extends CloudGitDeploymentDetector {

  private static final Pattern GIT_URL_PATTERN = Pattern.compile(Pattern.quote("git@heroku.com:") + "(.+)" + Pattern.quote(".git"));

  public HerokuDeploymentDetector() {
    super(GIT_URL_PATTERN);
  }

  @Override
  public ServerType getCloudType() {
    return HerokuCloudType.getInstance();
  }

  @Override
  public CloudDeploymentNameConfiguration createDeploymentConfiguration() {
    return new HerokuDeploymentConfiguration();
  }
}
