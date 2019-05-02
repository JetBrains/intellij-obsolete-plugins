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

package com.intellij.struts.dom.validator.converters;

import com.intellij.struts.StrutsManager;
import com.intellij.struts.ValidationModel;
import com.intellij.struts.dom.validator.Field;
import com.intellij.struts.dom.validator.Validator;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.ResolvingConverter;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class ValidatorNameConverter extends ResolvingConverter<Validator> {

  @Override
  @NotNull
  public Collection<? extends Validator> getVariants(final ConvertContext context) {
    final DomElement parent = context.getInvocationElement().getParent();
    assert parent != null;
    final Field field = (Field)parent.getParent();
    assert field != null;
    final List<Validator> validators = field.getDepends().getValue();
    if (validators == null) {
      return Collections.emptyList();
    }
    else {
      return validators;
    }
  }

  @Override
  public Validator fromString(final String s, final ConvertContext context) {
    if (s == null) {
      return null;
    }
    final ValidationModel validation = StrutsManager.getInstance().getValidation(context.getFile());
    return validation == null ? null : validation.findValidator(s);
  }

  @Override
  public String toString(final Validator validator, final ConvertContext context) {
    return validator == null ? null : validator.getName().getValue();
  }
}
