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

package com.intellij.struts;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.struts.dom.tiles.TilesDefinitions;
import com.intellij.struts.dom.validator.FormValidation;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an IDEA project with Struts support.
 *
 * @author Dmitry Avdeev
 */
public class StrutsProjectComponent {

  private final StrutsDomFactory myStrutsFactory;
  private final StrutsPluginDomFactory<TilesDefinitions, TilesModel> myTilesFactory;
  private final StrutsPluginDomFactory<FormValidation, ValidationModel> myValidatorFactory;

  @NotNull
  public static StrutsProjectComponent getInstance(@NotNull final Project project) {
    return ServiceManager.getService(project, StrutsProjectComponent.class);
  }

  /**
   * Init DOM-Factories.
   *
   * @param project IDEA project.
   */
  public StrutsProjectComponent(final Project project) {
    myStrutsFactory = new StrutsDomFactory(project);
    myTilesFactory = new TilesDomFactory(myStrutsFactory, project);
    myValidatorFactory = new ValidatorDomFactory(myStrutsFactory, project);
  }

  public StrutsDomFactory getStrutsFactory() {
    return myStrutsFactory;
  }

  public StrutsPluginDomFactory<TilesDefinitions, TilesModel> getTilesFactory() {
    return myTilesFactory;
  }

  public StrutsPluginDomFactory<FormValidation, ValidationModel> getValidatorFactory() {
    return myValidatorFactory;
  }
}
