/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

// Generated on Wed Apr 05 15:23:26 MSD 2006
// DTD/Schema  :    validator_1_2_0.dtd

package com.intellij.struts.dom.validator;

import com.intellij.ide.presentation.Presentation;
import com.intellij.struts.dom.StrutsRootElement;
import com.intellij.struts.dom.converters.StrutsElementNamer;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * validator_1_2_0.dtd:formset interface.
 * Type formset documentation
 * <pre>
 *       The "formset" element defines a set of forms for a locale. Formsets for
 *       specific locales can override only those fields that change. The
 *       localization is properly scoped, so that a formset can override just the
 *       language, or just the country, or both.
 * </pre>
 */
@Presentation(icon = "StrutsApiIcons.Validator.Formset",
              provider = StrutsElementNamer.class)
public interface Formset extends StrutsRootElement {

  /**
   * Returns the value of the variant child.
   * Attribute variant
   *
   * @return the value of the variant child.
   */
  @NotNull
  GenericAttributeValue<String> getVariant();


  /**
   * Returns the value of the country child.
   * Attribute country
   *
   * @return the value of the country child.
   */
  @NotNull
  GenericAttributeValue<String> getCountry();


  /**
   * Returns the value of the language child.
   * Attribute language
   *
   * @return the value of the language child.
   */
  @NotNull
  GenericAttributeValue<String> getLanguage();


  /**
   * Returns the list of constant children.
   * Type constant documentation
   * <pre>
   *      The "constant" element defines a static value that can be used as
   *      replacement parameters within "field" elements. The "constant-name" and
   *      "constant-value" elements define the constant's reference id and replacement
   *      value.
   * </pre>
   *
   * @return the list of constant children.
   */
  List<Constant> getConstants();

  /**
   * Adds new child to the list of constant children.
   *
   * @return created child
   */
  Constant addConstant();


  /**
   * Returns the list of form children.
   * Type form documentation
   * <pre>
   *      The "form" element defines a set of fields to be validated. The name
   *      corresponds to the identifier the application assigns to the form.
   * </pre>
   *
   * @return the list of form children.
   */
  @NotNull
  List<Form> getForms();

  /**
   * Adds new child to the list of form children.
   *
   * @return created child
   */
  Form addForm();


}
