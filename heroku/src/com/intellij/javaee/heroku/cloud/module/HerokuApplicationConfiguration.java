package com.intellij.javaee.heroku.cloud.module;

import com.intellij.javaee.heroku.cloud.HerokuAppTemplate;
import com.intellij.remoteServer.impl.module.CloudSourceApplicationConfiguration;

/**
 * @author michael.golubev
 */
public class HerokuApplicationConfiguration extends CloudSourceApplicationConfiguration {

  private final boolean myTemplate;
  private final HerokuAppTemplate myAppTemplate;

  protected HerokuApplicationConfiguration(boolean template, HerokuAppTemplate appTemplate, boolean existing, String existingAppName) {
    super(existing, existingAppName);
    myTemplate = template;
    myAppTemplate = appTemplate;
  }

  public boolean isTemplate() {
    return myTemplate;
  }

  public HerokuAppTemplate getTemplate() {
    return myAppTemplate;
  }
}
