package com.intellij.dmserver.integration;

import org.jetbrains.annotations.NonNls;

public class DMServerRepositoryExternalItem extends DMServerRepositoryItem20Base {

  @NonNls
  public static final String TYPE_PROPERTY_VALUE = "external";
  @NonNls
  private static final String PATH_PROPERTY_NAME = "searchPattern";

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
    return RepositoryPattern.create(this, pathResolver.path2Absolute(getPath()));
  }
}
