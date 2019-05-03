/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
