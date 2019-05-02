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
