package com.intellij.dmserver.deploy;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import javax.management.openmbean.CompositeData;

public class DeploymentIdentity {

  @NonNls
  private static final String COMPOSITE_ITEM_SYMBOLIC_NAME = "symbolicName";

  @NonNls
  private static final String COMPOSITE_ITEM_VERSION = "version";

  @NonNls
  private static final String COMPOSITE_ITEM_TYPE = "type";

  private final String mySymbolicName;
  private final String myVersion;
  private final String myType;
  private final String myRegion;

  public DeploymentIdentity(CompositeData compositeData) {
    this((String)compositeData.get(COMPOSITE_ITEM_SYMBOLIC_NAME),
         (String)compositeData.get(COMPOSITE_ITEM_VERSION),
         (String)compositeData.get(COMPOSITE_ITEM_TYPE));
  }

  public DeploymentIdentity(String symbolicName, String version, String type) {
    this(symbolicName, version, type, null);
  }

  public DeploymentIdentity(String symbolicName, String version, String type, String region) {
    mySymbolicName = symbolicName;
    myVersion = version;
    myType = type;
    myRegion = region;
  }

  public String getSymbolicName() {
    return mySymbolicName;
  }

  public String getVersion() {
    return myVersion;
  }

  public String getType() {
    return myType;
  }

  @Nullable
  public String getRegion() {
    return myRegion;
  }
}
