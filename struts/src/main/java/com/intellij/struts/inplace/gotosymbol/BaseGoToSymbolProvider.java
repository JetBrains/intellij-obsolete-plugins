/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.gotosymbol;

import com.intellij.facet.ProjectFacetManager;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.util.xml.model.gotosymbol.GoToSymbolProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public abstract class BaseGoToSymbolProvider extends GoToSymbolProvider {
  @Override
  protected boolean acceptModule(final Module module) {
    return !WebFacet.getInstances(module).isEmpty();
  }

  @NotNull
  @Override
  protected Collection<Module> calcAcceptableModules(@NotNull Project project) {
    return ProjectFacetManager.getInstance(project).getModulesWithFacet(WebFacet.ID);
  }
}
