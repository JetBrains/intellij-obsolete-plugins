/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.facet;

import com.intellij.facet.ui.FacetBasedFrameworkSupportProvider;
import com.intellij.facet.ui.libraries.LibraryInfo;
import com.intellij.ide.util.frameworkSupport.FrameworkVersion;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.struts.StrutsBundle;
import com.intellij.struts.facet.ui.StrutsVersion;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
public class StrutsFrameworkSupportProvider extends FacetBasedFrameworkSupportProvider<StrutsFacet> {
  private static final Logger LOG = Logger.getInstance("#com.intellij.struts.facet.StrutsFrameworkSupportProvider");

  public StrutsFrameworkSupportProvider() {
    super(StrutsFacetType.getInstance());
  }

  @Override
  public String getTitle() {
    return StrutsBundle.message("framework.title.struts");
  }

  @Override
  @NotNull
  public List<FrameworkVersion> getVersions() {
    List<FrameworkVersion> result = new ArrayList<>();
    for (StrutsVersion version : StrutsVersion.values()) {
      LibraryInfo[] jars = version.getJars();
      LibraryInfo strutsTaglib = version.getStrutsTaglib();
      if (version == StrutsVersion.Struts1_2_9 && strutsTaglib != null) {
        jars = ArrayUtil.append(jars, strutsTaglib, LibraryInfo.class);
      }
      result.add(new FrameworkVersion(version.toString(), "struts", jars));
    }
    return result;
  }

  private static StrutsVersion getVersion(String versionName) {
    for (StrutsVersion version : StrutsVersion.values()) {
      if (versionName.equals(version.toString())) {
        return version;
      }
    }
    LOG.error("invalid struts version: " + versionName);
    return null;
  }

  @Override
  protected void setupConfiguration(final StrutsFacet facet, final ModifiableRootModel rootModel, final FrameworkVersion version) {
  }

  @Override
  protected void onFacetCreated(final StrutsFacet facet, final ModifiableRootModel rootModel, final FrameworkVersion version) {
    StartupManager.getInstance(facet.getModule().getProject()).runWhenProjectIsInitialized(
      () -> AddStrutsSupportUtil.addSupportInWriteCommandAction(facet.getWebFacet(), false, false, getVersion(version.getVersionName())));
  }
}
