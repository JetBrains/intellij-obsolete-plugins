/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
