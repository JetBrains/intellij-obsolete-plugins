/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.reference.property;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.struts.StrutsManager;
import com.intellij.struts.StrutsModel;
import com.intellij.struts.ValidationModel;
import com.intellij.struts.dom.Action;
import com.intellij.struts.dom.FormBean;
import com.intellij.struts.inplace.reference.BaseReferenceProvider;
import org.jetbrains.annotations.Nullable;

/**
 * @author Dmitry Avdeev
 */
public class ValidatorFormPropertyReference extends FormPropertyReference {

  public ValidatorFormPropertyReference(final PropertyReferenceSet set, final int index, final TextRange range, BaseReferenceProvider provider) {
    super(set, index, range, false, provider);
  }

  @Override
  @Nullable
  protected StrutsModel getStrutsModel() {
    final ValidationModel validation = StrutsManager.getInstance().getValidation(myValue);
    return validation == null ? null : validation.getStrutsModel();
  }

  @Override
  @Nullable
  protected FormBean findFormBean(StrutsModel model, String name) {
    FormBean bean;
    if (StringUtil.startsWithChar(name, '/')) {
      final Action action = model.findAction(name);
      return action == null ? null : action.getName().getValue();
    } else {
      bean = model.findFormBean(name);
      if (bean == null) {
        // last try for BeanValidatorForm ...
        final Action action = model.findAction("/" + name);
        return action == null ? null : action.getName().getValue();
      }
      else {
        return bean;
      }
    }
  }
}
