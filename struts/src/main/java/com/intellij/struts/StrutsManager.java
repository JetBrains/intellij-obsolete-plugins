/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.struts.dom.StrutsConfig;
import com.intellij.struts.dom.StrutsRootElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NonNls;

import java.util.List;
import java.util.Set;

public abstract class StrutsManager {

  public static StrutsManager getInstance() {
    return ServiceManager.getService(StrutsManager.class);
  }

  @Nullable
  public abstract StrutsModel getStrutsModel(@Nullable PsiElement psiElement);

  @NotNull
  public abstract StrutsConfig getContext(@NotNull StrutsRootElement element);

  @NotNull
  public abstract List<StrutsModel> getAllStrutsModels(@NotNull Module module);

  @Nullable
  public abstract StrutsModel getCombinedStrutsModel(@Nullable Module module);

  @Nullable
  public abstract StrutsModel getModelByPrefix(@NotNull Module module, @NotNull @NonNls String modulePrefix);

  @Nullable
  public abstract StrutsConfig getStrutsConfig(PsiFile configFile);

  @NotNull
  public abstract Set<XmlFile> getStrutsConfigFiles(@Nullable PsiElement psiElement);


  @Nullable
  public abstract TilesModel getTiles(@Nullable PsiElement psiElement);

  @NotNull
  public abstract List<TilesModel> getAllTilesModels(@NotNull Module module);


  @Nullable
  public abstract ValidationModel getValidation(@Nullable PsiElement psiElement);

  @NotNull
  public abstract List<ValidationModel> getAllValidationModels(@NotNull Module module);

  @Nullable
  public abstract String getDefaultClassname(String attr, XmlTag tag);

  public abstract boolean isStrutsConfig(@NotNull XmlFile file);

}
