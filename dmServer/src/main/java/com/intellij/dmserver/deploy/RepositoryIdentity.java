package com.intellij.dmserver.deploy;

import org.jetbrains.annotations.NonNls;

import javax.management.openmbean.CompositeData;

public class RepositoryIdentity {

  @NonNls
  private static final String COMPOSITE_ITEM_NAME = "name";
  @NonNls
  private static final String COMPOSITE_ITEM_TYPE = "type";
  @NonNls
  private static final String COMPOSITE_ITEM_VERSION = "version";

  private final String myName;
  private final String myType;
  private final String myVersion;

  public RepositoryIdentity(CompositeData compositeData) {
    this((String)compositeData.get(COMPOSITE_ITEM_NAME), (String)compositeData.get(COMPOSITE_ITEM_TYPE),
         (String)compositeData.get(COMPOSITE_ITEM_VERSION));
  }

  public RepositoryIdentity(String name, String type, String version) {
    myName = name;
    myType = type;
    myVersion = version;
  }

  public String getName() {
    return myName;
  }

  public String getType() {
    return myType;
  }

  public String getVersion() {
    return myVersion;
  }
}
