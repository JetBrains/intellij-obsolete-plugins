package com.intellij.dmserver.editor;

import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.dmserver.util.ManifestUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.lang.manifest.psi.ManifestFile;
import org.osgi.framework.Constants;

import java.util.Collection;
import java.util.Collections;

public class ImportedBundlesPanel extends ImportedUnitsPanel {

  @Nls
  @Override
  protected String getListTitle() {
    return DmServerBundle.message("ImportedBundlesPanel.list.title");
  }

  @NonNls
  @Override
  protected String getHeaderName() {
    return ManifestUtils.IMPORT_BUNDLE_HEADER;
  }

  @Nls
  @Override
  protected String getAddDialogTitle() {
    return DmServerBundle.message("ImportedBundlesPanel.add.dialog.title");
  }

  @Nls
  @Override
  protected String getCollectingStatus() {
    return DmServerBundle.message("ImportedBundlesPanel.collecting.status");
  }

  @Override
  protected UnitsCollector getUnitsCollector(AvailableBundlesProvider provider) {
    return provider.getBundlesCollector();
  }

  @Override
  protected Collection<String> getOwnSymbolicNames(ManifestFile manifest) {
    String ownSymbolicName = ManifestUtils.getInstance().getHeaderValue(manifest, Constants.BUNDLE_SYMBOLICNAME);
    return ownSymbolicName == null ? Collections.emptyList() : Collections.singletonList(ownSymbolicName);
  }
}
