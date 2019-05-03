/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
