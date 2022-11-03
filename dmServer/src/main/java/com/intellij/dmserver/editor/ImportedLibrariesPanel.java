package com.intellij.dmserver.editor;

import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.dmserver.util.ManifestUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.lang.manifest.psi.ManifestFile;

import java.util.Collection;
import java.util.Collections;

public class ImportedLibrariesPanel extends ImportedUnitsPanel {

  @Nls
  @Override
  protected String getListTitle() {
    return DmServerBundle.message("ImportedLibrariesPanel.list.title");
  }

  @NonNls
  @Override
  protected String getHeaderName() {
    return ManifestUtils.IMPORT_LIBRARY_HEADER;
  }

  @Nls
  @Override
  protected String getAddDialogTitle() {
    return DmServerBundle.message("ImportedLibrariesPanel.add.dialog.title");
  }

  @Nls
  @Override
  protected String getCollectingStatus() {
    return DmServerBundle.message("ImportedLibrariesPanel.collecting.status");
  }

  @Override
  protected UnitsCollector getUnitsCollector(AvailableBundlesProvider provider) {
    return provider.getLibrariesCollector();
  }

  @Override
  protected Collection<String> getOwnSymbolicNames(ManifestFile manifest) {
    return Collections.emptyList();
  }
}
