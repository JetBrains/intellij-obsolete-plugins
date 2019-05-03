/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

// Generated on Wed Apr 05 15:23:26 MSD 2006
// DTD/Schema  :    validator_1_2_0.dtd

package com.intellij.struts.dom.validator;

import com.intellij.ide.presentation.Presentation;
import com.intellij.struts.dom.StrutsRootElement;
import com.intellij.struts.dom.validator.converters.DependsConverter;
import com.intellij.util.xml.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * validator_1_2_0.dtd:field interface.
 * Type field documentation
 * <pre>
 *      The "field" element defines the properties to be validated. In a
 *      web application, a field would also correspond to a control on
 *      a HTML form. To validate the properties, the validator works through
 *      a JavaBean representation. The field element accepts these
 *      attributes:
 *      property        The property on the JavaBean corresponding to this
 *                      field element.
 *      depends         The comma-delimited list of validators to apply against
 *                      this field. For the field to succeed, all the
 *                      validators must succeed.
 *      page            The JavaBean corresponding to this form may include
 *                      a page property. Only fields with a "page" attribute
 *                      value that is equal to or less than the page property
 *                      on the form JavaBean are processed. This is useful when
 *                      using a "wizard" approach to completing a large form,
 *                      to ensure that a page is not skipped.
 *                      [0]
 *      indexedListProperty
 *                      The "indexedListProperty" is the method name that will
 *                      return an array or a Collection used to retrieve the
 *                      list and then loop through the list performing the
 *                      validations for this field.
 * </pre>
 */
@Presentation(icon = "StrutsApiIcons.FormProperty")
public interface Field extends StrutsRootElement {

  /**
   * Returns the value of the indexedListProperty child.
   * Attribute indexedListProperty
   *
   * @return the value of the indexedListProperty child.
   */
  @NotNull
  GenericAttributeValue<String> getIndexedListProperty();


  /**
   * Returns the value of the page child.
   * Attribute page
   *
   * @return the value of the page child.
   */
  @NotNull
  GenericAttributeValue<String> getPage();


  /**
   * Returns the value of the property child.
   * Attribute property
   *
   * @return the value of the property child.
   */
  @NameValue(referencable = false, unique = false)
  @Required
  @NotNull
  GenericAttributeValue<String> getProperty();


  /**
   * Returns the value of the depends child.
   * Attribute depends
   *
   * @return the value of the depends child.
   */
  @Convert(DependsConverter.class)
  @NotNull
  GenericAttributeValue<List<Validator>> getDepends();


  /**
   * Returns the list of msg children.
   * Type msg documentation
   * <pre>
   *      The "msg" element defines a custom message key to use when one of the
   *      validators for this field fails. Each validator has a default message
   *      property that is used when a corresponding field msg is not specified.
   *      Each validator applied to a field may have its own msg element.
   *      The msg element accepts these attributes.
   *       name        The name of the validator corresponding to this msg.
   * <p/>
   *       bundle     The resource bundle name that the key should be resolved in.
   *       key         The key that will return the message template from a
   *                   resource bundle.
   *       resource    If set to "false", the key is taken to be a literal
   *                   value rather than a bundle key.
   *                   [true]
   * </pre>
   *
   * @return the list of msg children.
   */
  List<Msg> getMsgs();

  /**
   * Adds new child to the list of msg children.
   *
   * @return created child
   */
  Msg addMsg();


  /**
   * Returns the list of arg children.
   * Type arg documentation
   * <pre>
   *      The "arg" element defines a replacement value to use with the
   *      message template for this validator or this field.
   *      The arg element accepts these attributes.
   *       name        The name of the validator corresponding to this msg.
   *       			  If not supplied, this argument will be used in the given
   *       			  position for every validator.
   * <p/>
   *       bundle     The resource bundle name that the key should be resolved in.
   *       key         The key that will return the message template from a
   *                   resource bundle.
   *       resource    If set to "false", the key is taken to be a literal
   *                   value rather than a bundle key.
   *                   [true]
   *       position    The position of this replacement parameter in the message.
   *       			  For example, position="0" will set the first argument.
   *       			  [0]
   * </pre>
   *
   * @return the list of arg children.
   */
  List<Arg> getArgs();

  @SubTagList(value = "arg0")
  List<Arg> getArg0s();

  @SubTagList(value = "arg1")
  List<Arg> getArg1s();

  @SubTagList(value = "arg2")
  List<Arg> getArg2s();
  
  @SubTagList(value = "arg3")
  List<Arg> getArg3s();


  /**
   * Adds new child to the list of arg children.
   *
   * @return created child
   */
  Arg addArg();


  /**
   * Returns the list of var children.
   * Type var documentation
   * <pre>
   *      The "var" element can set parameters that a field may need to pass to
   *      one of its validators, such as the minimum and maximum values in a
   *      range validation. These parameters may also be referenced by one of the
   *      arg? elements using a shell syntax: ${var:var-name}.
   * </pre>
   *
   * @return the list of var children.
   */
  List<Var> getVars();

  /**
   * Adds new child to the list of var children.
   *
   * @return created child
   */
  Var addVar();


}
