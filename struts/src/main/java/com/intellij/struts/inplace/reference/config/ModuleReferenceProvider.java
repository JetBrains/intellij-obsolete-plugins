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

package com.intellij.struts.inplace.reference.config;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.struts.StrutsBundle;
import com.intellij.struts.StrutsManager;
import com.intellij.struts.StrutsModel;
import com.intellij.struts.inplace.reference.XmlAttributeReferenceProvider;
import com.intellij.struts.inplace.reference.XmlValueReference;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class ModuleReferenceProvider extends XmlAttributeReferenceProvider {

  private static final String CANONICAL = StrutsBundle.message("canonical.names.module");

  public ModuleReferenceProvider() {
    super(CANONICAL);
  }

  @Override
  protected PsiReference[] create(XmlAttributeValue attribute) {

    final XmlValueReference reference = new XmlValueReference(attribute, this) {
      @Override
      @Nullable
      protected PsiElement doResolve() {
        final String prefix = getValue();
        final Module module = ModuleUtilCore.findModuleForPsiElement(myValue);
        if (module != null) {
          final StrutsModel model = StrutsManager.getInstance().getModelByPrefix(module, prefix);
          if (model != null) {
            return model.getConfigurationTag();
          }
        }
        return null;
      }

      @Override
      @Nullable
      protected Object[] doGetVariants() {
        final Module module = ModuleUtilCore.findModuleForPsiElement(myValue);
        if (module != null) {
          final List<StrutsModel> models = StrutsManager.getInstance().getAllStrutsModels(module);
          final ArrayList<String> list = new ArrayList<>();
          for (StrutsModel model: models) {
            final String prefix = model.getModulePrefix();
            list.add(prefix);
          }
          return list.toArray();
        }
        return null;
      }

    };
    return new PsiReference[] { reference };
  }
}
