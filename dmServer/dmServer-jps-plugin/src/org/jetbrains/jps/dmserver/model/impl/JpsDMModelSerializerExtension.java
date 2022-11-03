package org.jetbrains.jps.dmserver.model.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.dmserver.model.JpsDMBundleArtifactType;
import org.jetbrains.jps.model.serialization.JpsModelSerializerExtension;
import org.jetbrains.jps.model.serialization.artifact.JpsArtifactDummyPropertiesSerializer;
import org.jetbrains.jps.model.serialization.artifact.JpsArtifactPropertiesSerializer;
import org.jetbrains.jps.model.serialization.artifact.JpsPackagingElementSerializer;

import java.util.Collections;
import java.util.List;

/**
 * @author michael.golubev
 */
public class JpsDMModelSerializerExtension extends JpsModelSerializerExtension {

  @NotNull
  @Override
  public List<? extends JpsPackagingElementSerializer<?>> getPackagingElementSerializers() {
    return Collections.singletonList(new JpsDMContainerPackagingElementSerializer());
  }

  @NotNull
  @Override
  public List<? extends JpsArtifactPropertiesSerializer<?>> getArtifactTypePropertiesSerializers() {
    return Collections
      .singletonList(new JpsArtifactDummyPropertiesSerializer(JpsDMBundleArtifactType.TYPE_ID, JpsDMBundleArtifactType.DM_BUNDLE));
  }
}
