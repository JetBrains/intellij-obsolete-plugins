/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.reference.config;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.struts.StrutsManager;
import com.intellij.struts.ValidationModel;
import com.intellij.struts.dom.validator.Validator;
import com.intellij.struts.inplace.reference.ListAttrReferenceProvider;
import com.intellij.struts.inplace.reference.XmlValueReference;
import com.intellij.util.xml.ElementPresentationManager;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Reference to validator.
 */
public class ValidatorReferenceProvider extends ListAttrReferenceProvider {

  @NonNls
  public final static String CANONICAL = "validator";

  public ValidatorReferenceProvider() {
    super(CANONICAL, null, false);
  }

  @Override
  @NotNull
  protected XmlValueReference create(final XmlAttributeValue attribute, final TextRange range) {
    return new XmlValueReference(attribute, this, range) {

      @Override
      @Nullable
      protected XmlTag doResolve() {
        final ValidationModel model = StrutsManager.getInstance().getValidation(attribute);
        if (model == null) {
          return null;
        }

        final String name = getValue();
        final Validator validator = model.findValidator(name);
        return validator == null ? null : validator.getXmlTag();
      }

      @Override
      @Nullable
      protected Object[] doGetVariants() {
        final ValidationModel model = StrutsManager.getInstance().getValidation(attribute);
        if (model == null) {
          return null;
        }

        final List<Validator> validators = model.getValidators();
        return ElementPresentationManager.getInstance().createVariants(validators);
      }

    };
  }

}