package com.intellij.dmserver.artifacts.plan;

import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import org.osgi.framework.VersionRange;

/**
 * @author michael.golubev
 */
public interface PlanArtifactElement extends PlanElementBase {

  GenericAttributeValue<DMArtifactElementType> getType();

  @Convert(ArtifactNameConverter.class)
  GenericAttributeValue<String> getName();

  @Convert(VersionRangeConverter.class)
  GenericAttributeValue<VersionRange> getVersion();
}
