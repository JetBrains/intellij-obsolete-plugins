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

import com.intellij.psi.PsiElement;
import com.intellij.struts.StrutsManager;
import com.intellij.struts.ValidationModel;
import com.intellij.struts.dom.validator.Validator;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.ElementPresentationManager;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.converters.DelimitedListConverter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class DependsConverter extends DelimitedListConverter<Validator> {

  public DependsConverter() {
    super(", ");
  }

  @Override
  protected Validator convertString(final @Nullable String string, final ConvertContext context) {
    if (string == null) {
      return null;
    }
    final ValidationModel validation = StrutsManager.getInstance().getValidation(context.getFile());
    return validation == null ? null : validation.findValidator(string);
  }

  @Override
  protected String toString(final @Nullable Validator validator) {
    return validator == null ? null : validator.getName().getStringValue();
  }

  @Override
  protected Object[] getReferenceVariants(final ConvertContext context,
                                          final GenericDomValue<? extends List<Validator>> genericDomValue) {
    final ValidationModel validation = StrutsManager.getInstance().getValidation(context.getFile());
    if (validation != null) {
      final List<Validator> variants = validation.getValidators();
      filterVariants(variants, genericDomValue);
      return ElementPresentationManager.getInstance().createVariants(variants);
    }
    return EMPTY_ARRAY;
  }

  @Override
  protected PsiElement resolveReference(@Nullable final Validator validator, final ConvertContext context) {
    return validator == null ? null : validator.getXmlTag();
  }

  @Override
  protected String getUnresolvedMessage(final String value) {
    return "Cannot resolve validator '" + value + "'";
  }

}