package com.intellij.dmserver.editor;

import java.util.List;

public interface UnitsCollector {

  List<ExportedUnit> getAvailableUnits();

  boolean isUnitAvailable(String symbolicName);
}
