package org.jetbrains.jps.dmserver.model;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.jps.model.JpsDummyElement;
import org.jetbrains.jps.model.artifact.JpsArtifactType;
import org.jetbrains.jps.model.ex.JpsElementTypeWithDummyProperties;

/**
 * @author michael.golubev
 */
public final class JpsDMBundleArtifactType extends JpsElementTypeWithDummyProperties implements JpsArtifactType<JpsDummyElement> {

  @NonNls
  public static final String TYPE_ID = "dm.bundle";

  @NonNls
  public static final String JAR_EXTENSION = "jar";
  @NonNls
  public static final String WAR_EXTENSION = "war";

  public static final JpsDMBundleArtifactType DM_BUNDLE = new JpsDMBundleArtifactType();

  private JpsDMBundleArtifactType() {
  }
}
