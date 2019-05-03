/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.facet.ui;

import com.intellij.facet.Facet;
import com.intellij.facet.ui.libraries.FacetLibrariesValidatorDescription;
import com.intellij.javaee.artifact.JavaeeArtifactUtil;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.struts.facet.StrutsFacet;
import org.jetbrains.annotations.NotNull;

/**
 * @author nik
*/
public class StrutsLibrariesValidatorDescription extends FacetLibrariesValidatorDescription {
  public StrutsLibrariesValidatorDescription() {
    super("struts");
  }

  @Override
  public void onLibraryAdded(final Facet facet, @NotNull final Library library) {
    JavaeeArtifactUtil.getInstance().addLibraryToAllArtifactsContainingFacet(library, ((StrutsFacet)facet).getWebFacet());
  }
}
