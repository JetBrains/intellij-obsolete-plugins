package com.intellij.dmserver.facet;

import com.intellij.ide.util.frameworkSupport.FrameworkVersion;
import com.intellij.javaee.facet.JavaeeFacet;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.javaee.web.facet.WebFacetFrameworkSupportProvider;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.packaging.artifacts.Artifact;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DMWebFacetFrameworkSupportProvider extends WebFacetFrameworkSupportProvider {

  public void addSupport(@NotNull WebFacet facet, @NotNull ModifiableRootModel rootModel, @Nullable String versionName) {
    FrameworkVersion version = findVersion(versionName);
    super.setupConfiguration(facet, rootModel, version);
    super.onFacetCreated(facet, rootModel, version);
  }

  public boolean isCreateConfigFile(String versionString) {
    return getVersionToCreate(versionString) != null;
  }

  private FrameworkVersion findVersion(String versionName) {
    FrameworkVersion defaultVersion = null;
    for (FrameworkVersion version : getVersions()) {
      if (version.getVersionName().equals(versionName)) {
        return version;
      }
      if (version.isDefault()) {
        defaultVersion = version;
      }
    }
    return defaultVersion;
  }

  @Override
  protected Artifact createJavaeeArtifact(JavaeeFacet facet) {
    // suppress JavaEE artifact creation - we have our own
    return null; // should never be called
  }
}
