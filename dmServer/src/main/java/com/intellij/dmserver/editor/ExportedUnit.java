package com.intellij.dmserver.editor;

import org.osgi.framework.Version;

public interface ExportedUnit {

  String getSymbolicName();

  Version getVersion();
}
