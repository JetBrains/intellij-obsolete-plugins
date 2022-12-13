package com.intellij.dmserver;

import org.jetbrains.annotations.NonNls;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public final class DMConstants {
  private DMConstants() {
  }

  public static final int DEFAULT_PORT = 8080;

  @NonNls
  public static final String DEFAULT_MBEAN_SERVER_USERNAME = "admin";

  public static final int DEFAULT_MBEAN_SERVER_PORT = 9875;

  @NonNls
  public static final String DEFAULT_MBEAN_SERVER_PASSWORD = "springsource";

  public static final ObjectName MBEAN_BUNDLE_ADMIN;

  static {
    try {
      @NonNls String bundleAdmin = "com.springsource.server:type=BundleAdmin";
      MBEAN_BUNDLE_ADMIN = ObjectName.getInstance(bundleAdmin);
    }
    catch (MalformedObjectNameException e) {
      throw new InternalError("Never happens");
    }
  }
}
