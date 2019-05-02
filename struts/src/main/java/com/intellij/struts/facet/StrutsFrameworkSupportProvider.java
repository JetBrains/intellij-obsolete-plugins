/*
 * Copyright 2000-2007 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
