/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
