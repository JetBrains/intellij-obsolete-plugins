/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts;

import com.intellij.javaee.model.xml.web.Servlet;
import com.intellij.javaee.model.xml.web.ServletMapping;
import com.intellij.javaee.model.xml.web.WebApp;
import com.intellij.javaee.web.CommonParamValue;
import com.intellij.javaee.web.ServletMappingInfo;
import com.intellij.javaee.web.ServletMappingType;
import com.intellij.javaee.web.WebUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.jsp.WebDirectoryUtil;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts.dom.StrutsConfig;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Dmitry Avdeev
 */
public class StrutsDomFactory extends WebDomFactory<StrutsConfig, StrutsModel> {

  @NonNls private static final String CONFIG = "config";
  @NonNls private static final String CONFIG_PREFIX = "config/";

  public StrutsDomFactory(Project project) {
    super(StrutsConfig.class, project, "Struts");

  }

  @Override
  @Nullable
  protected List<StrutsModel> computeAllModels(@NotNull Module module) {
    Collection<WebFacet> webFacets = WebFacet.getInstances(module);
    final ArrayList<StrutsModel> models = new ArrayList<>();
    for (WebFacet webFacet : webFacets) {
      final List<StrutsModel> strutsModels = computeAllModels(webFacet);
      if (strutsModels != null) {
        models.addAll(strutsModels);
      }
    }
    return models;
  }

  @Override
  protected StrutsModel computeModel(@NotNull XmlFile psiFile, @Nullable Module module) {
    final StrutsModel model = super.computeModel(psiFile, module);
    if (model != null) {
      return model;
    }
    final WebFacet webFacet = WebUtil.getWebFacet(psiFile);
    if (webFacet == null) {
      return null;
    }
    final ServletMappingInfo info = findStrutsMapping(webFacet);
    if (info != null) {
      final DomFileElement<StrutsConfig> dom = getDomRoot(psiFile);
      if (dom != null) {
        return new StrutsModelImpl(Collections.singleton(psiFile), dom, info, "/", null);
      }
    }
    return null;
  }

  @Nullable
  private List<StrutsModel> computeAllModels(@NotNull WebFacet webFacet) {
    final ServletMappingInfo mappingInfo = findStrutsMapping(webFacet);
    if (mappingInfo == null) {
      return null;
    }
    List<? extends CommonParamValue> parameters = mappingInfo.getServlet().getInitParams();
    final List<StrutsModel> strutsModels = new ArrayList<>();
    params: for (CommonParamValue parameter : parameters) {
      String parameterName = parameter.getParamName().getValue();
      if (parameterName == null) {
        continue;
      }

      String modulePath = null;
      if (parameterName.startsWith(CONFIG_PREFIX)) {
        modulePath = parameterName.substring(CONFIG_PREFIX.length() - 1);
      } else if (CONFIG.equals(parameterName)) {
        modulePath = "/";
      }

      if (modulePath != null) {
        // check path uniquness
        for (StrutsModel model: strutsModels) {
          if (model.getModulePrefix().equals(modulePath)) {
            continue params;
          }
        }
        String configList = parameter.getParamValue().getValue();
        if (configList == null) {
          continue;
        }
        Set<XmlFile> configFiles = new LinkedHashSet<>();
        String[] paths = configList.split(",");
        for (String path : paths) {
          PsiElement file = WebDirectoryUtil.getWebDirectoryUtil(webFacet.getModule().getProject()).findFileByPath(path.trim(), webFacet);
          if (file instanceof XmlFile) {
            configFiles.add((XmlFile)file);
          }
        }
        if (configFiles.size() > 0) {
          final DomFileElement<StrutsConfig> mergedModel = createMergedModelRoot(configFiles);

          if (mergedModel != null) {
          final PsiElement tag = parameter.getIdentifyingPsiElement();
          assert tag != null;
          StrutsModel model = new StrutsModelImpl(configFiles, mergedModel, mappingInfo, modulePath, tag);
          strutsModels.add(model);
        }
        }
      }
    }
    return strutsModels;
  }

  @Nullable
  private static ServletMappingInfo findStrutsMapping(@NotNull WebFacet webFacet) {
    WebApp webApp = webFacet.getRoot();
    if (webApp == null) {
      return null;
    }

    Servlet actionServlet = findActionServlet(webApp);
    if (actionServlet == null) {
      return null;
    }
    ServletMapping strutsMapping = null;
    for (ServletMapping mapping: webApp.getServletMappings()) {
      if (actionServlet.equals(mapping.getServletName().getValue())) {
        strutsMapping = mapping;
        break;
      }
    }
    if (strutsMapping == null) {
      return null;
    }
    final List<GenericDomValue<String>> list = strutsMapping.getUrlPatterns();
    if (list.size() == 0) {
      return null;
    }
    final String urlPattern = list.get(0).getStringValue();
    if (urlPattern == null) {
      return null;
    }
    ServletMappingInfo mappingInfo = ServletMappingType.getPatternType(urlPattern).createMappingInfo(urlPattern, strutsMapping);
    if (mappingInfo.getType() == ServletMappingType.EXACT) {
      return null;
    }
    return mappingInfo;
  }

  @Override
  protected StrutsModel createCombinedModel(@NotNull final Set<XmlFile> configFiles, @NotNull final DomFileElement<StrutsConfig> mergedModel, final StrutsModel firstModel,
                                            final Module module) {
    return new StrutsModelImpl(configFiles,
                               mergedModel, firstModel.getServletMappingInfo(),
                               "/",
                               firstModel.getConfigurationTag());
  }

  @Nullable
  private static Servlet findActionServlet(@NotNull WebApp webApp) {

    PsiClass actionServletClass = null;
    List<Servlet> servlets = webApp.getServlets();
    for (Servlet servlet : servlets) {
      PsiClass servletPsiClass = servlet.getServletClass().getValue();
      if (servletPsiClass != null) {
        if (actionServletClass == null) {
          final GlobalSearchScope scope = servlet.getResolveScope();
          actionServletClass =
            JavaPsiFacade.getInstance(servletPsiClass.getProject()).findClass(StrutsConstants.ACTION_SERVLET_CLASS, scope);
          if (actionServletClass == null) {
            return null;
          }
        }
        if (InheritanceUtil.isInheritorOrSelf(servletPsiClass, actionServletClass, true)) {
          return servlet;
        }
      }
      final String stringValue = servlet.getServletClass().getStringValue();
      if (stringValue != null && stringValue.equals(StrutsConstants.ACTION_SERVLET_CLASS)) {
        return servlet;
      }
    }

    return null;
  }
}
