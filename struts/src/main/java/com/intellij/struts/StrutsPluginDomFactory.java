/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts;

import com.intellij.javaee.web.WebUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.jsp.WebDirectoryUtil;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.struts.dom.PlugIn;
import com.intellij.struts.dom.SetProperty;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomFileElement;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Dmitry Avdeev
 */
public abstract class StrutsPluginDomFactory<T extends DomElement, M extends NamedDomModel<T>> extends WebDomFactory<T, M> {

  private final String mySuperClass;
  private final String myConfigProperty;
  protected final StrutsDomFactory myStrutsFactory;

  protected StrutsPluginDomFactory(@NotNull Class<T> aClass,
                                   @NotNull String superClass,
                                   @NotNull String configProperty,
                                   @NotNull StrutsDomFactory strutsFactory,
                                   Project project,
                                   @NonNls String name) {

    super(aClass, project, name);
    myStrutsFactory = strutsFactory;
    mySuperClass = superClass;
    myConfigProperty = configProperty;
  }

  @Nullable
  private PlugIn getPlugin(@NotNull StrutsModel struts) {
    PsiClass superClass = null;
    for (final PlugIn plugin : struts.getMergedModel().getPlugIns()) {
      final PsiClass pluginClass = plugin.getClassName().getValue();
      if (pluginClass != null) {
        if (superClass == null) {
          superClass = JavaPsiFacade.getInstance(pluginClass.getProject()).findClass(mySuperClass, pluginClass.getResolveScope());
          if (superClass == null) {
            return null;
          }
        }
        if (InheritanceUtil.isInheritorOrSelf(pluginClass, superClass, true)) {
          return plugin;
        }
      }
    }
    return null;
  }

  @Override
  @Nullable
  public List<M> computeAllModels(@NotNull final Module module) {

    final List<StrutsModel> strutsModels = myStrutsFactory.getAllModels(module);
    if (strutsModels.size() == 0) {
      return Collections.emptyList();
    }

    final ArrayList<M> list = new ArrayList<>(strutsModels.size());

    for (final StrutsModel strutsModel : strutsModels) {
      final M model = getModelFromStruts(strutsModel);
      if (model != null) {
        list.add(model);
      }
    }
    return list;
  }

  @Nullable
  protected M getModelFromStruts(@NotNull final StrutsModel strutsModel) {
    final PlugIn plugin = getPlugin(strutsModel);
    if (plugin != null) {
      final Set<XmlFile> configFiles = new LinkedHashSet<>();
      final XmlTag element = plugin.getXmlTag();
      assert element != null;
      final WebFacet webFacet = WebUtil.getWebFacet(element);
      if (webFacet == null) {
        return null;
      }
      final WebDirectoryUtil webDirectoryUtil = WebDirectoryUtil.getWebDirectoryUtil(plugin.getManager().getProject());

      for (final SetProperty prop : plugin.getSetProperties()) {
        final String name = prop.getProperty().getStringValue();
        if (myConfigProperty.equals(name)) {
          final String configString = prop.getValue().getStringValue();
          if (configString != null) {
            final String[] configPaths = configString.split(",");
            for (final String configPath : configPaths) {
              final PsiElement psiElement = resolveFile(configPath, webDirectoryUtil, webFacet);
              if (psiElement instanceof XmlFile) {
                configFiles.add((XmlFile) psiElement);
              }
            }
          }
        }
      }
      if (configFiles.size() > 0) {
        final DomFileElement<T> mergedModel = createMergedModelRoot(configFiles);
        return mergedModel == null ? null : createModel(configFiles, mergedModel, strutsModel);
      }
    }
    return null;
  }

  protected abstract M createModel(@NotNull Set<XmlFile> configFiles, @NotNull DomFileElement<T> mergedModel, StrutsModel strutsModel);

  @Nullable
  protected PsiElement resolveFile(final String path, final WebDirectoryUtil webDirectoryUtil, final WebFacet webFacet) {
    return webDirectoryUtil.findFileByPath(path.trim(), webFacet);
  }
}
