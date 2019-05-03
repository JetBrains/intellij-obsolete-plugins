/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.generate;

import com.intellij.openapi.editor.Editor;
import com.intellij.struts.StrutsBundle;
import com.intellij.struts.dom.FormBean;
import com.intellij.struts.dom.FormBeans;
import com.intellij.struts.dom.StrutsConfig;
import com.intellij.util.xml.DomElement;
import icons.StrutsApiIcons;
import org.jetbrains.annotations.Nullable;

/**
 * @author Dmitry Avdeev
 */
public class GenerateFormAction extends GenerateMappingAction<FormBean> {

  public GenerateFormAction() {

    super(new GenerateMappingProvider<FormBean>(StrutsBundle.message("generate.form"), FormBean.class, "struts-form-bean", FormBeans.class, StrutsConfig.class) {

      @Override
      public FormBean generate(@Nullable final DomElement parent, final Editor editor) {
        final FormBeans mappings;
        if (parent instanceof StrutsConfig) {
          mappings = ((StrutsConfig)parent).getFormBeans();
          mappings.ensureTagExists();
        }
        else if (parent instanceof FormBeans) {
          mappings = (FormBeans)parent;
        }
        else {
          return null;
        }
        return mappings.addFormBean();
      }

    }, StrutsApiIcons.FormBean);
  }

}
