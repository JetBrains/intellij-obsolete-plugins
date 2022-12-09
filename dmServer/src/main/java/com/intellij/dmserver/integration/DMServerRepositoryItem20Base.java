package com.intellij.dmserver.integration;

import org.jetbrains.annotations.NonNls;

import java.util.Properties;

public abstract class DMServerRepositoryItem20Base extends DMServerRepositoryItemBase {

  @NonNls
  public static final String TYPE_PROPERTY_NAME = "type";

  public static final String PROPERTY_NAME_SPLITTER = ".";

  private String myName;

  public String getName() {
    return myName;
  }

  public void setName(String name) {
    myName = name;
  }

  public void load(Properties properties) {
    setPath(properties.getProperty(getFullPropertyName(getPathPropertyName())));
  }

  public void save(Properties properties) {
    properties.setProperty(getFullPropertyName(TYPE_PROPERTY_NAME), getTypePropertyValue());

    properties.setProperty(getFullPropertyName(getPathPropertyName()), getPath());
  }

  protected final String getFullPropertyName(@NonNls String propertyName) {
    return getName() + PROPERTY_NAME_SPLITTER + propertyName;
  }

  @NonNls
  protected abstract String getPathPropertyName();

  @NonNls
  protected abstract String getTypePropertyValue();

}
