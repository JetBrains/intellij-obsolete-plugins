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

package com.intellij.struts.facet;

import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.struts.StrutsManager;
import com.intellij.util.descriptors.ConfigFile;
import org.jetbrains.annotations.Nullable;

/**
 * @author Dmitry Avdeev
 */
public class StrutsSupportModel {

  private boolean struts13;
  private boolean tiles;

  private boolean strutsLib;
  private boolean validation;
  private boolean strutsEl;

  private boolean strutsTaglib;
  private boolean strutsFaces;
  private boolean scripting;
  private boolean extras;

  private boolean jstl;


  private StrutsSupportModel() {
  }

  /**
   * @param webFacet WebModule
   */
  public static StrutsSupportModel checkStrutsSupport(@Nullable WebFacet webFacet) {
    StrutsSupportModel support = new StrutsSupportModel();
    if (webFacet == null) {
      return support;
    }
    ConfigFile desc = webFacet.getWebXmlDescriptor();
    if (desc == null) {
      return support;
    }

    support.tiles = StrutsManager.getInstance().getAllTilesModels(webFacet.getModule()).size() > 0;

    support.validation = StrutsManager.getInstance().getAllValidationModels(webFacet.getModule()).size() > 0;

    Project project = webFacet.getModule().getProject();
    final PsiManager psiManager = PsiManager.getInstance(project);
    final GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(webFacet.getModule());
    support.strutsLib = JavaPsiFacade.getInstance(psiManager.getProject()).findClass("org.apache.struts.action.Action", scope) != null;
    support.struts13 = JavaPsiFacade.getInstance(psiManager.getProject())
      .findClass("org.apache.struts.chain.ComposableRequestProcessor", scope) != null;
    support.jstl = JavaPsiFacade.getInstance(psiManager.getProject())
      .findClass("javax.servlet.jsp.jstl.core.ConditionalTagSupport", scope) != null;
    support.strutsEl = JavaPsiFacade.getInstance(psiManager.getProject()).findClass("org.apache.strutsel.taglib.html.ELBaseTag", scope) != null;
    support.strutsTaglib = JavaPsiFacade.getInstance(psiManager.getProject()).findClass("org.apache.struts.taglib.html.BaseTag", scope) != null;
    support.extras = JavaPsiFacade.getInstance(psiManager.getProject())
      .findClass("org.apache.struts.plugins.ModuleConfigVerifier", scope) != null;
    support.scripting = JavaPsiFacade.getInstance(psiManager.getProject()).findClass("org.apache.struts.scripting.ScriptAction", scope) != null;
    support.strutsFaces = JavaPsiFacade.getInstance(psiManager.getProject())
      .findClass("org.apache.struts.faces.application.FacesRequestProcessor", scope) != null;

    return support;
  }

  public boolean isTiles() {
    return tiles;
  }

  public boolean isStrutsLib() {
    return strutsLib;
  }

  public boolean isValidation() {
    return validation;
  }

  public boolean isStrutsEl() {
    return strutsEl;
  }

  public boolean isStrutsTaglib() {
    return strutsTaglib;
  }

  public boolean isStrutsFaces() {
    return strutsFaces;
  }

  public boolean isScripting() {
    return scripting;
  }

  public boolean isExtras() {
    return extras;
  }

  public boolean isJstl() {
    return jstl;
  }

  public boolean isStruts13() {
    return struts13;
  }
}
