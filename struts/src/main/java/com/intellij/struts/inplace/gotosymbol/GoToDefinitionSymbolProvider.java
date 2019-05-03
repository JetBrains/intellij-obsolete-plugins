/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.gotosymbol;

import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.module.Module;
import com.intellij.struts.StrutsManager;
import com.intellij.struts.TilesModel;
import com.intellij.struts.dom.tiles.Definition;
import com.intellij.util.xml.model.gotosymbol.GoToSymbolProvider;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * GoTo Tiles definition support.
 */
public class GoToDefinitionSymbolProvider extends BaseGoToSymbolProvider {

  @Override
  protected void addNames(@NotNull Module module, Set<String> result) {
    List<TilesModel> tilesModels = StrutsManager.getInstance().getAllTilesModels(module);
    for (TilesModel model : tilesModels) {
      addNewNames(model.getDefinitions(), result);
    }
  }

  @Override
  protected void addItems(@NotNull Module module, String name, List<NavigationItem> result) {
    List<TilesModel> tilesModels = StrutsManager.getInstance().getAllTilesModels(module);
    for (TilesModel model : tilesModels) {
      final Definition value = model.findDefinition(name);
      if (value != null) {
        final NavigationItem item = GoToSymbolProvider.createNavigationItem(value);
        if (item != null) {
          result.add(item);
        }
      }
    }
  }

}