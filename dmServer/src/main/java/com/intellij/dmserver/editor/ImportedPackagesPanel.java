package com.intellij.dmserver.editor;

import com.intellij.dmserver.util.DmServerBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.osgi.framework.Constants;
import org.jetbrains.lang.manifest.psi.ManifestFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ImportedPackagesPanel extends ImportedUnitsPanel {

  @Nls
  @Override
  protected String getListTitle() {
    return DmServerBundle.message("ImportedPackagesPanel.list.title");
  }

  @NonNls
  @Override
  protected String getHeaderName() {
    return Constants.IMPORT_PACKAGE;
  }

  @Nls
  @Override
  protected String getAddDialogTitle() {
    return DmServerBundle.message("ImportedPackagesPanel.add.dialog.title");
  }

  @Nls
  @Override
  protected String getCollectingStatus() {
    return DmServerBundle.message("ImportedPackagesPanel.collecting.status");
  }

  @Override
  protected UnitsCollector getUnitsCollector(AvailableBundlesProvider provider) {
    return provider.getPackagesCollector();
  }

  @Override
  protected Collection<String> getOwnSymbolicNames(ManifestFile manifest) {
    List<String> result = new ArrayList<>();
    for (ExportedUnit exportedPackage : AvailableBundlesProvider.getExportedPackages(manifest)) {
      result.add(exportedPackage.getSymbolicName());
    }
    return result;
  }
}
