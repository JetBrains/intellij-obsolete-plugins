/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

// Generated on Wed Apr 05 15:29:47 MSD 2006
// DTD/Schema  :    validator_1_2_0.dtd

package com.intellij.struts.dom.validator;


import com.intellij.ide.presentation.Presentation;
import com.intellij.struts.dom.StrutsRootElement;

import java.util.List;

/**
 * validator_1_2_0.dtd:global interface.
 * Type global documentation
 * <pre>
 *     The elements defined here are all global and must be nested within a
 *     "global" element.
 * </pre>
 */
@Presentation(icon = "StrutsApiIcons.Validator.Global")
public interface Global extends StrutsRootElement {

  /**
   * Returns the list of validator children.
   * Type validator documentation
   * <pre>
   *      The "validator" element defines what validator objects can be used with
   *      the fields referenced by the formset elements.
   *      elements:
   *        validator         Defines a new validatior
   *        javascript        The javascript source code for client side validation.
   *      attributes:
   *          name            The name of this validation
   *          classname       The java class name that handles server side validation
   *          method          The java method that handles server side validation
   *          methodParams    The java class types passed to the serverside method
   *          msg             a generic message key to use when this validator fails.
   *                          It can be overridden by the 'msg' element for a specific field.
   *          depends         The comma-delimited list of validator that are called before this validator.
   *                          For this validation to succeed, all the listed validators must succeed.
   *          jsFunctionName  The name of the javascript function which returns all fields of a certain type.
   *          jsFunction      The name of the javascript function which is passed the form for validation.
   * </pre>
   *
   * @return the list of validator children.
   */
  List<Validator> getValidators();

  /**
   * Adds new child to the list of validator children.
   *
   * @return created child
   */
  Validator addValidator();


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


}
