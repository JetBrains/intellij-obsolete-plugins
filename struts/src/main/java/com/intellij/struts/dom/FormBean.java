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
import com.intellij.util.xml.*;
import com.intellij.struts.dom.converters.StrutsBooleanConverter;
import com.intellij.struts.dom.converters.NameConverter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * struts-config_1_3.dtd:form-bean interface.
 * Type form-bean documentation
 * <pre>
 *  The "form-bean" element describes an ActionForm subclass
 *      [org.apache.struts.action.ActionForm] that can be referenced by an "action"
 *      element.
 * The "form-bean" element describes a particular form bean, which is a
 *      JavaBean that implements the org.apache.struts.action.ActionForm
 *      class.  The following attributes are defined:
 *      className       The configuration bean for this form bean object. If
 *                      specified, the object must be a subclass of the default
 *                      configuration bean.
 *                      ["org.apache.struts.config.FormBeanConfig"]
 *      extends         The name of the form bean that this bean will
 *                      inherit configuration information from.
 *      name            The unique identifier for this form bean. Referenced by the
 *                      <action> element to specify which form bean to use with its
 *                      request.
 * <p/>
 *      type            Fully qualified Java class name of the ActionForm subclass
 *                      to use with this form bean.
 * <p/>
 *      enhanced        Flag indicating if the form bean should be dynamically
 *                      enhanced to include getters and setters for defined
 *                      properties. This is only valid when 'type' is a
 *                      dynamic form bean (an instance of
 *                      "org.apache.struts.action.DynaActionForm" or a sub-class,
 *                      and requires CGLIB when enabled.
 * </pre>
 */
@Presentation(icon = "StrutsApiIcons.FormBean")
public interface FormBean extends StrutsRootElement {

  /**
   * Returns the value of the name child.
   * Attribute name
   *
   * @return the value of the name child.
   */
  @Stubbed
  @NameValue
  @NotNull
  @Required
  @Referencing(NameConverter.ForForm.class)
  GenericAttributeValue<String> getName();


  /**
   * Returns the value of the type child.
   * Attribute type
   *
   * @return the value of the type child.
   */
  @Stubbed
  @NotNull
  @ExtendClass("org.apache.struts.action.ActionForm")
  @Required
  GenericAttributeValue<PsiClass> getType();


  /**
   * Returns the value of the className child.
   * Attribute className
   *
   * @return the value of the className child.
   */
  @ExtendClass("org.apache.struts.config.FormBeanConfig")
  @NotNull
  GenericAttributeValue<PsiClass> getClassName();


  /**
   * Returns the value of the enhanced child.
   * Attribute enhanced
   *
   * @return the value of the enhanced child.
   */
  @NotNull
  @Convert(StrutsBooleanConverter.class)
  GenericAttributeValue<Boolean> getEnhanced();


  /**
   * Returns the value of the extends child.
   * Attribute extends
   *
   * @return the value of the extends child.
   */
  @NotNull
  GenericAttributeValue<FormBean> getExtends();


  /**
   * Returns the value of the icon child.
   * Type icon documentation
   * <pre>
   *  The "icon" element contains a small-icon and large-icon element which
   *      specify the location, relative to the Struts configuration file, for small
   *      and large images used to represent the surrounding element in GUI tools.
   * </pre>
   *
   * @return the value of the icon child.
   */
  Icon getIcon();


  /**
   * Returns the value of the display-name child.
   * Type display-name documentation
   * <pre>
   *  The "display-name" element contains a short (one line) description of
   *      the surrounding element, suitable for use in GUI tools.
   * </pre>
   *
   * @return the value of the display-name child.
   */
  GenericDomValue<String> getDisplayName();


  /**
   * Returns the value of the description child.
   * Type description documentation
   * <pre>
   *  The "description" element contains descriptive (paragraph length) text
   *      about the surrounding element, suitable for use in GUI tools.
   * </pre>
   *
   * @return the value of the description child.
   */
  GenericDomValue<String> getDescription();


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


  /**
   * Returns the list of form-property children.
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
   *
   * @return the list of form-property children.
   */
  @Stubbed
  List<FormProperty> getFormProperties();

  /**
   * Adds new child to the list of form-property children.
   *
   * @return created child
   */
  FormProperty addFormProperty();


}
