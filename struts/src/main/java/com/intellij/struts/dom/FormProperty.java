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
// DTD/Schema  :    struts-config_1_3.dtd

package com.intellij.struts.dom;

import com.intellij.ide.presentation.Presentation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiType;
import com.intellij.util.xml.*;
import com.intellij.struts.dom.converters.StrutsBooleanConverter;
import com.intellij.struts.dom.converters.FormPropertyTypeConverter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * struts-config_1_3.dtd:form-property interface.
 * Type form-property documentation
 * <pre>
 *  The "form-property" element describes a JavaBean property that can be used to
 *      configure an instance of a DynaActionForm or a subclass thereof. This element
 *      is only utilized when the "type" attribute of the enclosing "form-bean" element
 *      is [org.apache.struts.action.DynaActionForm] or a subclass of DynaActionForm. If
 *      a custom DynaActionForm subclass is used, then the "dynamic" attribute of the
 *      enclosing <form-bean> element must be set to "true". Since Struts 1.1.
 *      className       The configuration bean for this form property object. If
 *                      specified, the object must be a subclass of the default
 *                      configuration bean.
 *                      ["org.apache.struts.config.FormPropertyConfig"]
 *      initial         String representation of the initial value for this property.
 *                      If not specified, primitives will be initialized to zero and
 *                      objects initialized to the zero-argument instantiation of that
 *                      object class.  For example, Strings will be initialized to ""
 *      name            The name of the JavaBean property described by this element.
 *      reset           The flag that indicates when this property should be reset
 *                      to its "initial" value when the form's "reset()" method is
 *                      called.  If this is set to "true", the property is always
 *                      reset when "reset()" is called.  This can also be set to
 *                      one or more HTTP methods, such as GET or POST. In such a
 *                      case, the property will be reset only when the HTTP method
 *                      used for the request being processed is included in this
 *                      attribute's value(s).  Multiple HTTP methods can be
 *                      specified by separating them with whitespace or commas.
 *      size            The number of array elements to create if the value of the
 *                      "type" attribute specifies an array, but there is no value
 *                      specified for the "initial" attribute.
 *      type            Fully qualified Java class name of the field underlying this
 *                      property, optionally followed by "[]" to indicate that the
 *                      field is indexed.
 * </pre>
 */
@Presentation(icon = "StrutsApiIcons.FormProperty")
public interface FormProperty extends StrutsRootElement {

  /**
   * Returns the value of the name child.
   * Attribute name
   *
   * @return the value of the name child.
   */
  @Stubbed
  @NotNull
  @NameValue
  @Required
  GenericAttributeValue<String> getName();


  /**
   * Returns the value of the initial child.
   * Attribute initial
   *
   * @return the value of the initial child.
   */
  @NotNull
  GenericAttributeValue<String> getInitial();


  /**
   * Returns the value of the size child.
   * Attribute size
   *
   * @return the value of the size child.
   */
  @NotNull
  GenericAttributeValue<Integer> getSize();


  /**
   * Returns the value of the type child.
   * Attribute type
   *
   * @return the value of the type child.
   */
  @Stubbed
  @NotNull
  @Required
  @Convert(FormPropertyTypeConverter.class)
  GenericAttributeValue<PsiType> getType();


  /**
   * Returns the value of the className child.
   * Attribute className
   *
   * @return the value of the className child.
   */
  @ExtendClass("org.apache.struts.config.FormPropertyConfig")
  @NotNull
  GenericAttributeValue<PsiClass> getClassName();


  /**
   * Returns the value of the reset child.
   * Attribute reset
   *
   * @return the value of the reset child.
   */
  @Convert(StrutsBooleanConverter.class)
  @NotNull
  GenericAttributeValue<Boolean> getReset();


  /**
   * Returns the list of set-property children.
   * Type set-property documentation
   * <pre>
   *  The "set-property" element specifies the method name and initial value of
   *      an additional JavaBean configuration property. When the object representing
   *      the surrounding element is instantiated, the accessor for the indicated
   *      property is called and passed the indicated value. The "set-property"
   *      element is especially useful when a custom subclass is used with
   *      <forward>, <action>, or <plug-in> elements. The subclass
   *      can be passed whatever other properties may be required to configure the
   *      object without changing how the struts-config is parsed.
   *     Since Struts 1.3, an alternate syntax is supported.  By using
   *     the "key" attribute instead of the "property" attribute, you can set
   *     arbitrary string properties on the Config object which is populated
   *     based on the containing element.   NOTE: the "key" attribute is NOT
   * 	supported for <set-property> inside a <plug-in> element.
   *      property        Name of the JavaBeans property whose setter method
   *                      will be called. Exactly one of
   *                      "property" or "key" must be specified.
   *      key             Where supported, the key which will be used to store
   *                      the specified value in the given config object.  Exactly one of
   *                      "property" or "key" must be specified.
   *      value           String representation of the value to which this
   *                      property will be set, after suitable type conversion
   * </pre>
   *
   * @return the list of set-property children.
   */
  List<SetProperty> getSetProperties();

  /**
   * Adds new child to the list of set-property children.
   *
   * @return created child
   */
  SetProperty addSetProperty();


}
