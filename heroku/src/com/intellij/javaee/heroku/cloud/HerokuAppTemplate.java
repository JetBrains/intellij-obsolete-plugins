package com.intellij.javaee.heroku.cloud;

/**
 * @author michael.golubev
 */
public class HerokuAppTemplate {

  private final String myName;

  public HerokuAppTemplate(String name) {
    myName = name;
  }

  public String getGitUrl() {
    return "https://github.com/heroku/" + myName + ".git";
  }

  @Override
  public String toString() {
    return myName;
  }
}
