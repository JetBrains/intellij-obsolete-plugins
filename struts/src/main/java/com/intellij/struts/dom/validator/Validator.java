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

// Generated on Wed Apr 05 15:23:26 MSD 2006
// DTD/Schema  :    validator_1_2_0.dtd

package com.intellij.struts.dom.validator;

import com.intellij.ide.presentation.Presentation;
import com.intellij.psi.PsiClass;
import com.intellij.struts.dom.StrutsRootElement;
import com.intellij.util.xml.*;
import org.jetbrains.annotations.NotNull;

/**
 * validator_1_2_0.dtd:validator interface.
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
 */
@Presentation(icon = "StrutsApiIcons.Validator.Validator")
public interface Validator extends StrutsRootElement {

  /**
   * Returns the value of the methodParams child.
   * Attribute methodParams
   *
   * @return the value of the methodParams child.
   */
  @NotNull
  GenericAttributeValue<String> getMethodParams();


  /**
   * Returns the value of the name child.
   * Attribute name
   *
   * @return the value of the name child.
   */
  @NotNull
  @NameValue
  GenericAttributeValue<String> getName();


  /**
   * Returns the value of the jsFunctionName child.
   * Attribute jsFunctionName
   *
   * @return the value of the jsFunctionName child.
   */
  @NotNull
  GenericAttributeValue<String> getJsFunctionName();


  /**
   * Returns the value of the classname child.
   * Attribute classname
   *
   * @return the value of the classname child.
   */
  @NotNull
  @ExtendClass(allowEmpty = true, allowAbstract = true, instantiatable = false)
  GenericAttributeValue<PsiClass> getClassname();


  /**
   * Returns the value of the depends child.
   * Attribute depends
   *
   * @return the value of the depends child.
   */
  @NotNull
  GenericAttributeValue<String> getDepends();


  /**
   * Returns the value of the msg child.
   * Attribute msg
   *
   * @return the value of the msg child.
   */
  @NotNull
  GenericAttributeValue<String> getMsg();


  /**
   * Returns the value of the jsFunction child.
   * Attribute jsFunction
   *
   * @return the value of the jsFunction child.
   */
  @NotNull
  GenericAttributeValue<String> getJsFunction();


  /**
   * Returns the value of the method child.
   * Attribute method
   *
   * @return the value of the method child.
   */
  @NotNull
  GenericAttributeValue<String> getMethod();


  /**
   * Returns the value of the javascript child.
   * Type javascript documentation
   * <pre>
   *      The "javascript" element defines a JavaScript that can be used to perform
   *      client-side validators.
   * </pre>
   *
   * @return the value of the javascript child.
   */
  GenericDomValue<String> getJavascript();


}
