package com.intellij.dmserver.artifacts.plan;

import com.intellij.dmserver.util.VersionUtils;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.Converter;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.VersionRange;

/**
 * @author michael.golubev
 */
public class VersionRangeConverter extends Converter<VersionRange> {
  @Override
  public VersionRange fromString(@Nullable String s, ConvertContext context) {
    return VersionUtils.parseVersionRange(s);
  }

  @Override
  public String toString(@Nullable VersionRange versionRange, ConvertContext context) {
    return versionRange == null ? null : versionRange.toString();
  }
}
