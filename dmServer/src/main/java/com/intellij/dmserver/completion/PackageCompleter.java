package com.intellij.dmserver.completion;

import com.intellij.dmserver.editor.AvailableBundlesProvider;
import com.intellij.dmserver.editor.UnitsCollector;
import org.osgi.framework.Constants;

public class PackageCompleter extends UnitCompleter {

  @Override
  public String getHeaderName() {
    return Constants.IMPORT_PACKAGE;
  }

  @Override
  protected UnitsCollector getUnitsCollector(AvailableBundlesProvider provider) {
    return provider.getPackagesCollector();
  }
}
