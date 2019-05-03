/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.psi;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.jsp.WebDirectoryUtil;
import com.intellij.psi.jsp.WebDirectoryElement;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.struts.*;
import com.intellij.struts.dom.StrutsConfig;
import com.intellij.struts.dom.StrutsRootElement;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomManager;
import com.intellij.util.xml.ExtendClass;
import com.intellij.util.xml.reflect.DomAttributeChildDescription;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class StrutsManagerImpl extends StrutsManager {

  @Override
  @Nullable
  public StrutsModel getStrutsModel(@Nullable final PsiElement psiElement) {
    if (psiElement == null) {
      return null;
    }
    final PsiElement p = psiElement.getParent();
    if (p == null) {
      return null;
    }
    final PsiElement parent = p.getParent();
    if (parent instanceof XmlTag) {
      final String modulePrefix = ((XmlTag)parent).getAttributeValue("module");
      if (modulePrefix != null) {
        final Module module = ModuleUtil.findModuleForPsiElement(psiElement);
        if (module != null) {
          final StrutsModel model = getModelByPrefix(module, modulePrefix);
          if (model != null) {
            return model;
          }
        }
      }
    }
    PsiFile file = psiElement.getContainingFile();
    if (file == null) {
      return null;
    }
    return getModelByFile(file);
  }

  @Override
  @NotNull
  public StrutsConfig getContext(@NotNull final StrutsRootElement element) {
    final XmlElement xmlElement = element.getXmlElement();
    final StrutsModel model = getStrutsModel(xmlElement);
    assert xmlElement != null;
    final StrutsConfig strutsConfig = model == null ? getStrutsConfig(xmlElement.getContainingFile()) : model.getMergedModel();
    assert strutsConfig != null;
    return strutsConfig;
  }

  @Nullable
  private StrutsModel getModelByFile(PsiFile file) {
    file = file.getOriginalFile();
    final FileType fileType = file.getFileType();
    if (fileType == StdFileTypes.XML) {
      return StrutsProjectComponent.getInstance(file.getProject()).getStrutsFactory().getModel(file);
    } else {
      final Module module = ModuleUtilCore.findModuleForPsiElement(file);
      if (module != null) {
        final VirtualFile virtualFile = file.getVirtualFile();
        assert virtualFile != null;
        final WebDirectoryElement dir = WebDirectoryUtil.findParentWebDirectory(module.getProject(), virtualFile);
        if (dir != null) {
          final List<StrutsModel> strutsModels = getAllStrutsModels(module);
          String path = dir.getPath();
          while (true) {
            for (StrutsModel model: strutsModels) {
              final WebDirectoryElement moduleRoot = model.getModuleRoot();
              if (moduleRoot != null && path.equals(moduleRoot.getPath())) {
                return model;
              }
            }
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash == -1) {
              break;
            }
            path = path.substring(0, lastSlash);
          }
        }
        return getCombinedStrutsModel(module);
      }
    }
    return null;
  }

  @Override
  @NotNull
  public List<StrutsModel> getAllStrutsModels(@NotNull final Module module) {
    return StrutsProjectComponent.getInstance(module.getProject()).getStrutsFactory().getAllModels(module);
  }

  @Override
  @Nullable
  public StrutsConfig getStrutsConfig(@NotNull final PsiFile configFile) {
    return configFile instanceof XmlFile ? StrutsProjectComponent.getInstance(configFile.getProject()).getStrutsFactory().getDom((XmlFile)configFile) : null;
  }

  @Override
  @Nullable
  public StrutsModel getCombinedStrutsModel(@Nullable final Module module) {
    return module == null ? null : StrutsProjectComponent.getInstance(module.getProject()).getStrutsFactory().getCombinedModel(module);
  }

  @Override
  @Nullable
  public StrutsModel getModelByPrefix(@NotNull final Module module, @NotNull @NonNls final String modulePrefix) {
    final List<StrutsModel> models = getAllStrutsModels(module);
    for (StrutsModel model : models) {
      if (model.getModulePrefix().equals(modulePrefix)) {
        return model;
      }
    }
    return null;
  }

  @Override
  @Nullable
  public ValidationModel getValidation(@Nullable final PsiElement psiElement) {
    return psiElement == null ? null : StrutsProjectComponent.getInstance(psiElement.getProject()).getValidatorFactory().getModel(psiElement);
  }

  @Override
  @NotNull
  public List<ValidationModel> getAllValidationModels(@NotNull final Module module) {
    return StrutsProjectComponent.getInstance(module.getProject()).getValidatorFactory().getAllModels(module);
  }

  @Override
  @Nullable
  public String getDefaultClassname(final String attrName, final XmlTag tag) {
    final DomElement domElement = DomManager.getDomManager(tag.getProject()).getDomElement(tag);
    if (domElement != null) {
      final DomAttributeChildDescription<?> childDescription = domElement.getGenericInfo().getAttributeChildDescription(attrName);
      if (childDescription != null) {
        final ExtendClass annotation = childDescription.getAnnotation(ExtendClass.class);
        if (annotation != null) {
          return annotation.value();
        }
      }
    }
    return null;
  }

  @Override
  @Nullable
  public TilesModel getTiles(@Nullable final PsiElement psiElement) {
    return psiElement == null ? null : StrutsProjectComponent.getInstance(psiElement.getProject()).getTilesFactory().getModel(psiElement);
  }

  @Override
  @NotNull
  public List<TilesModel> getAllTilesModels(@NotNull final Module module) {
    return StrutsProjectComponent.getInstance(module.getProject()).getTilesFactory().getAllModels(module);
  }

  @Override
  public boolean isStrutsConfig(@NotNull final XmlFile file) {
    return DomManager.getDomManager(file.getProject()).getFileElement(file, StrutsConfig.class) != null;
  }

  @Override
  @NotNull
  public Set<XmlFile> getStrutsConfigFiles(@Nullable final PsiElement psiElement) {
    final StrutsModel model = getStrutsModel(psiElement);
    if (model == null) {
      return Collections.emptySet();
    }
    else {
      return model.getConfigFiles();
    }
  }

}
