package com.intellij.dmserver.editor;

import org.osgi.framework.VersionRange;

/**
 * @author michael.golubev
 */
public interface ImportedUnit {
  String getSymbolicName();

  VersionRange getVersionRange();

  void setVersionRange(VersionRange versionRange);

  boolean isOptional();

  void setOptional(boolean optional);
}
