/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts;

import com.intellij.javaee.web.CommonServlet;
import com.intellij.javaee.web.ServletMappingInfo;
import com.intellij.javaee.web.WebUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.jsp.WebDirectoryUtil;
import com.intellij.psi.jsp.WebDirectoryElement;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts.core.PsiBeanProperty;
import com.intellij.struts.core.PsiBeanPropertyCache;
import com.intellij.struts.core.PsiBeanPropertyImpl;
import com.intellij.struts.dom.*;
import com.intellij.struts.util.PsiClassUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Dmitry Avdeev
 */
public class StrutsModelImpl extends NamedModelImpl<StrutsConfig> implements StrutsModel {

  private final CommonServlet myActionServlet;
  private final ServletMappingInfo myMappingInfo;
  @Nullable private final PsiElement myConfigurationTag;

  public StrutsModelImpl(@NotNull Set<XmlFile> configFiles, @NotNull DomFileElement<StrutsConfig> mergedModel, @NotNull ServletMappingInfo mappingInfo, @NotNull String modulePrefix,
                         @Nullable PsiElement configurationTag) {

    super(configFiles, mergedModel, modulePrefix);
    myMappingInfo = mappingInfo;
    myActionServlet = mappingInfo.getServlet();
    myConfigurationTag = configurationTag;
  }

  @Override
  @NotNull
  public String getModulePrefix() {
    return getName();
  }

  @Override
  @Nullable
  public WebDirectoryElement getModuleRoot() {
    final WebFacet webFacet = WebUtil.getWebFacet(myActionServlet);
    final WebDirectoryUtil directoryUtil = WebDirectoryUtil.getWebDirectoryUtil(getMergedModel().getManager().getProject());
    return directoryUtil.findWebDirectoryElementByPath(getModulePrefix(), webFacet);
  }

  @Override
  @Nullable
  public PsiElement getConfigurationTag() {
    return myConfigurationTag;
  }

  @Override
  @NotNull
  public ServletMappingInfo getServletMappingInfo() {
    return myMappingInfo;
  }

  @Override
  @NotNull
  public List<Action> getActions() {
    return getMergedModel().getActionMappings().getActions();
  }

  @Override
  @Nullable
  public Action findAction(@NotNull String actionPath) {
    return DomUtil.findByName(getActions(), actionPath);
  }

  @Override
  @Nullable
  public Action resolveActionURL(String actionURL) {
    String actionPath = getActionName(actionURL);
    return actionPath == null ? findAction(actionURL) : findAction(actionPath);
  }

  @Override
  @Nullable
  public String getActionName(String url) {
    final ServletMappingInfo info = getServletMappingInfo();
    final String trimmedUrl = WebUtil.trimURL(url);
    return info.stripMapping(trimmedUrl);
  }

  @Override
  @NotNull
  public List<FormBean> getFormBeans() {
    return getMergedModel().getFormBeans().getFormBeans();
  }

  @Override
  @Nullable
  public FormBean findFormBean(String formBeanName) {
    return DomUtil.findByName(getFormBeans(), formBeanName);
  }

  @Override
  @NotNull
  public List<Forward> getGlobalForwards() {
    return getMergedModel().getGlobalForwards().getForwards();
  }

  @Override
  @Nullable
  public Forward findForward(String forwardName) {
    return DomUtil.findByName(getGlobalForwards(), forwardName);
  }

  @Override
  public boolean isInputForward() {
    return false;
  }

  @Override
  @NotNull
  public CommonServlet getActionServlet() {
    return myActionServlet;
  }

  @Override
  @NotNull
  public PsiBeanProperty[] getFormProperties(@NotNull final FormBean form) {
    PsiClass type = form.getType().getValue();
    if (type != null) {
        if (PsiClassUtil.isSuper(type, "org.apache.struts.action.DynaActionForm")) {
          // analyze form here
          List<FormProperty> tags = form.getFormProperties();
          ArrayList<PsiBeanProperty> props = new ArrayList<>();
          for (FormProperty tag : tags) {
            PsiBeanProperty prop = PsiBeanPropertyImpl.create(tag);
            ContainerUtil.addIfNotNull(props, prop);
          }
          return props.toArray(PsiBeanProperty.EMPTY_ARRAY);
        }
        else {
          return PsiBeanPropertyCache.getInstance(type.getProject()).getBeanProperties(type);
        }
    }
    return PsiBeanProperty.EMPTY_ARRAY;
  }
}
