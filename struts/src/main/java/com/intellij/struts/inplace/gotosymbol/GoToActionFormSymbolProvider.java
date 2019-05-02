/*
 * Copyright 2000-2007 JetBrains s.r.o.
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