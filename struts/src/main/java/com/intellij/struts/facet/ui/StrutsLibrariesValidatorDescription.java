/*
 * Copyright 2000-2006 JetBrains s.r.o.
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
