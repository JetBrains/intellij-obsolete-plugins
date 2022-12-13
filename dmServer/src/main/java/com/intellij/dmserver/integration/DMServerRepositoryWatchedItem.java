package com.intellij.dmserver.integration;

import org.jetbrains.annotations.NonNls;

import java.util.Collections;
import java.util.Properties;

public class DMServerRepositoryWatchedItem extends DMServerRepositoryItem20Base {

  @NonNls
  public static final String TYPE_PROPERTY_VALUE = "watched";
  @NonNls
  private static final String PATH_PROPERTY_NAME = "watchDirectory";
  @NonNls
  private static final String WATCHED_INTERVAL_PROPERTY_NAME = "watchedInterval";

  private String myWatchedInterval;

  public String getWatchedInterval() {
    return myWatchedInterval;
  }

  public void setWatchedInterval(String watchedInterval) {
    myWatchedInterval = watchedInterval;
  }

  @Override
  public void load(Properties properties) {
    super.load(properties);
    setWatchedInterval(properties.getProperty(getFullPropertyName(WATCHED_INTERVAL_PROPERTY_NAME)));
  }

  @Override
  public void save(Properties properties) {
    super.save(properties);
    if (getWatchedInterval() != null) {
      properties.setProperty(getFullPropertyName(WATCHED_INTERVAL_PROPERTY_NAME), getWatchedInterval());
    }
  }

  @NonNls
  @Override
  protected String getPathPropertyName() {
    return PATH_PROPERTY_NAME;
  }

  @Override
  protected String getTypePropertyValue() {
    return TYPE_PROPERTY_VALUE;
  }

  @Override
  public RepositoryPattern createPattern(PathResolver pathResolver) {
    return new RepositoryPattern(this, pathResolver.path2Absolute(getPath()), Collections.emptyList(), RepositoryPattern.ANY_FILE);
  }
}
