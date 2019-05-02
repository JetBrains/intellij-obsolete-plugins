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

import com.intellij.javaee.web.CommonServlet;
import com.intellij.javaee.web.ServletMappingInfo;
import com.intellij.psi.PsiElement;
import com.intellij.psi.jsp.WebDirectoryElement;
import com.intellij.struts.core.PsiBeanProperty;
import com.intellij.struts.dom.Action;
import com.intellij.struts.dom.FormBean;
import com.intellij.struts.dom.Forward;
import com.intellij.struts.dom.StrutsConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public interface StrutsModel extends NamedDomModel<StrutsConfig> {

  /**
   * Struts module prefix including starting slash, ("/" for default module).
   *
   * @return module prefix.
   */
  @NotNull
  String getModulePrefix();

  @Nullable
  WebDirectoryElement getModuleRoot();

  @Nullable
  PsiElement getConfigurationTag();

  @NotNull
  ServletMappingInfo getServletMappingInfo();

  @NotNull
  List<Action> getActions();

  /**
   * @param actionPath action "path" attribute without parameters and extensions, e.g. "/login"
   * @return null if the action not found
   * @see #resolveActionURL(String)
   */
  @Nullable
  Action findAction(@NotNull String actionPath);

  /**
   * @param actionURL action URL, e.g. "/login.do?start=true" or "/do/login". Parameters will be stripped.
   * @return null if the action not found
   * @see #getActionName(String)
   * @see #findAction(String)
   */
  @Nullable
  Action resolveActionURL(String actionURL);

  @Nullable
  String getActionName(String url);

  @NotNull
  List<FormBean> getFormBeans();

  @Nullable
  FormBean findFormBean(String formBeanName);

  @NotNull
  List<Forward> getGlobalForwards();

  @Nullable
  Forward findForward(String forwardName);

  boolean isInputForward();

  @NotNull
  CommonServlet getActionServlet();

  @NotNull
  PsiBeanProperty[] getFormProperties(@NotNull FormBean form);

}
