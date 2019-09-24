package com.intellij.javaee.heroku.agent;

import com.heroku.api.App;
import com.intellij.remoteServer.agent.util.CloudGitApplication;

/**
 * @author michael.golubev
 */
public class HerokuApplicationImpl implements CloudGitApplication {

  private final String myName;
  private final String myGitUrl;
  private final String myWebUrl;

  public HerokuApplicationImpl(App app) {
    myName = app.getName();
    myGitUrl = app.getGitUrl();
    myWebUrl = app.getWebUrl();
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public String getGitUrl() {
    return myGitUrl;
  }

  @Override
  public String getWebUrl() {
    return myWebUrl;
  }
}
