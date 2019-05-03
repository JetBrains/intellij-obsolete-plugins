/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.gotosymbol;

import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.module.Module;
import com.intellij.struts.StrutsManager;
import com.intellij.struts.StrutsModel;
import com.intellij.struts.dom.FormBean;
import com.intellij.util.xml.model.gotosymbol.GoToSymbolProvider;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * GoTo ActionForm support.
 */
public class GoToActionFormSymbolProvider extends BaseGoToSymbolProvider {

  @Override
  protected void addNames(@NotNull final Module module, final Set<String> result) {
    List<StrutsModel> strutsModels = StrutsManager.getInstance().getAllStrutsModels(module);
    for (StrutsModel model : strutsModels) {
      addNewNames(model.getFormBeans(), result);
    }
  }

  @Override
  protected void addItems(@NotNull final Module module, final String name, final List<NavigationItem> result) {
    List<StrutsModel> strutsModels = StrutsManager.getInstance().getAllStrutsModels(module);
    for (StrutsModel model : strutsModels) {
      FormBean value = model.findFormBean(name);
      if (value != null) {
        final NavigationItem item = GoToSymbolProvider.createNavigationItem(value);
        if (item != null) {
          result.add(item);
        }
      }
    }
  }

}