/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.highlighting;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.project.Project;
import com.intellij.struts.StrutsProjectComponent;
import com.intellij.struts.facet.StrutsFacet;
import com.intellij.util.xml.model.impl.DomModelFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Provides {@link TilesInspection} on make.
 *
 * @author Dmitry Avdeev
 */
public class TilesValidator extends StrutsValidatorBase {

  public TilesValidator() {
    super("Tiles Model validator", "Validating Tiles model...");
  }

  @NotNull
  @Override
  public Class<? extends LocalInspectionTool>[] getInspectionToolClasses(CompileContext context) {
    return new Class[]{TilesInspection.class};
  }

  @Override
  protected DomModelFactory getFactory(Project project) {
    return StrutsProjectComponent.getInstance(project).getTilesFactory();
  }

  @Override
  protected boolean isAvailableOnFacet(final StrutsFacet facet) {
    return facet.getConfiguration().getValidationConfiguration().myTilesValidationEnabled;
  }

}
