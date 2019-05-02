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
