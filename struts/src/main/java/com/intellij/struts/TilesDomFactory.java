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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts.dom.tiles.TilesDefinitions;
import com.intellij.struts.psi.TilesModelImpl;
import com.intellij.util.xml.DomFileElement;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Dmitry Avdeev
*/
public class TilesDomFactory extends StrutsPluginDomFactory<TilesDefinitions, TilesModel> {

  @NonNls private static final String TILES_PLUGIN_CLASS = "org.apache.struts.tiles.TilesPlugin";
  @NonNls private static final String DEFINITIONS_CONFIG_PROPERTY = "definitions-config";

    public TilesDomFactory(final StrutsDomFactory strutsFactory, final Project project) {
    super(TilesDefinitions.class,
          TILES_PLUGIN_CLASS,
          DEFINITIONS_CONFIG_PROPERTY,
          strutsFactory, project,
          "Tiles");
  }

  @Override
  public List<TilesModel> computeAllModels(@NotNull Module module) {
    ArrayList<TilesModel> models = new ArrayList<>(super.computeAllModels(module));
    for (TilesModelProvider provider : TilesModelProvider.EP_NAME.getExtensionList()) {
      models.addAll(provider.computeModels(module));
    }
    return models;
  }

  /**
   * Returns model for given element
   *
   * @param psiElement element from Tiles config, Struts config, or JSP
   * @return model for given element
   */
  @Override
  public TilesModel getModel(@NotNull final PsiElement psiElement) {
    final TilesModel model = super.getModel(psiElement);
    if (model == null) {
      final PsiFile psiFile = psiElement.getContainingFile();
      if (psiFile instanceof XmlFile) {
        final StrutsModel strutsModel = myStrutsFactory.getModelByConfigFile((XmlFile)psiFile);
        if (strutsModel != null) {
          return getModelFromStruts(strutsModel);
        }
      }
    }
    if (model == null) {
      final Module module = ModuleUtilCore.findModuleForPsiElement(psiElement);
      if (module != null) {
        return getCombinedModel(module);
      }
    }
    return model;
  }

  @Override
  protected TilesModel createModel(@NotNull final Set<XmlFile> configFiles, @NotNull final DomFileElement<TilesDefinitions> mergedModel, final StrutsModel strutsModel) {
    return new TilesModelImpl(configFiles, mergedModel, strutsModel.getName());
  }

  @Override
  protected TilesModel createCombinedModel(@NotNull final Set<XmlFile> configFiles, @NotNull final DomFileElement<TilesDefinitions> mergedModel,
                                           final TilesModel firstModel, final Module module) {
    return new TilesModelImpl(configFiles, mergedModel, "/");
  }
}
