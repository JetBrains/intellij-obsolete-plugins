package com.intellij.dmserver.artifacts;

import com.intellij.dmserver.util.ManifestUtils;
import com.intellij.openapi.command.WriteCommandAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.lang.manifest.psi.Header;
import org.jetbrains.lang.manifest.psi.ManifestFile;

/**
 * @author michael.golubev
 */
public class ManifestUpdater {

  private final ManifestFile myManifestFile;

  public ManifestUpdater(@NotNull ManifestFile manifestFile) {
    myManifestFile = manifestFile;
  }

  public void updateHeader(final @NotNull String headerName, final @NotNull String headerValue, final boolean keepExistingValue) {
    WriteCommandAction.runWriteCommandAction(myManifestFile.getProject(), () -> {
  Header header = ManifestUtils.getInstance().findHeader(myManifestFile, headerName);
  if (keepExistingValue && header != null && ManifestUtils.getInstance().getHeaderValue(header) != null) {
    return;
  }
  Header newHeader = ManifestUtils.getInstance().createHeader(myManifestFile.getProject(), headerName, headerValue);
  if (header == null) {
    ManifestUtils.getInstance().addHeader(myManifestFile, newHeader);
  }
  else {
    header.replace(newHeader);
  }
});
  }
}
