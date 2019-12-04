/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.tiles.springMvc;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.pom.references.PomService;
import com.intellij.psi.PsiElement;
import com.intellij.spring.contexts.model.SpringModel;
import com.intellij.spring.web.mvc.model.SpringMVCModel;
import com.intellij.spring.web.mvc.views.ViewResolver;
import com.intellij.struts.StrutsPluginDomFactory;
import com.intellij.struts.StrutsProjectComponent;
import com.intellij.struts.TilesModel;
import com.intellij.struts.dom.tiles.Definition;
import com.intellij.struts.dom.tiles.TilesDefinitions;
import com.intellij.util.xml.DomTarget;
import icons.StrutsApiIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class TilesViewResolver extends ViewResolver {

  private final StrutsPluginDomFactory<TilesDefinitions, TilesModel> myTilesFactory;
  private final String myID;
  private final Module myModule;

  TilesViewResolver(String ID, SpringModel model) {
    myID = ID;
    myModule = model.getModule();
    assert myModule != null;

    myTilesFactory = StrutsProjectComponent.getInstance(myModule.getProject()).getTilesFactory();
  }

  @NotNull
  @Override
  public String getID() {
    return myID;
  }

  @Override
  public PsiElement resolveView(@NotNull PsiElement context, final String viewName, final SpringMVCModel springMVCModel) {
    final Definition definition = findDefinition(viewName);
    if (definition == null) return null;

    DomTarget target = DomTarget.getTarget(definition);
    if (target == null) return null;
    
    return PomService.convertToPsi(target);
  }

  @Override
  public List<LookupElement> getAllViews(@NotNull PsiElement context, SpringMVCModel springMVCModel) {
    final TilesModel tilesModel = getTilesModel();
    if (tilesModel == null) {
      return Collections.emptyList();
    }

    final List<LookupElement> lookupElements = new ArrayList<>();
    for (Definition definition : tilesModel.getDefinitions()) {
      DomTarget target = DomTarget.getTarget(definition);
      if (target == null || target.getName() == null) continue;

      final PsiElement resolvePsiElement = target.getNavigationElement();

      final LookupElementBuilder lookupElement =
        LookupElementBuilder.create(resolvePsiElement, target.getName())
          .withIcon(StrutsApiIcons.Tiles.Tile)
          .withTypeText(resolvePsiElement.getContainingFile().getName());
      lookupElements.add(lookupElement);
    }
    return lookupElements;
  }

  @Override
  public String bindToElement(PsiElement element) {
    return null;
  }

  @NotNull
  @Override
  public String handleElementRename(String newElementName) {
    return newElementName;
  }

  @Nullable
  private TilesModel getTilesModel() {
    return myTilesFactory.getCombinedModel(myModule);
  }

  @Nullable
  private Definition findDefinition(String viewName) {
    final TilesModel model = getTilesModel();
    if (model == null) return null;

    return model.findDefinition(viewName);
  }
}
