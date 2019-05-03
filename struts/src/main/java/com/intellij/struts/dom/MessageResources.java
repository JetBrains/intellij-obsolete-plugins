/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

// Generated on Wed Apr 05 15:23:26 MSD 2006
// DTD/Schema  :    struts-config_1_3.dtd

package com.intellij.struts.dom;

import com.intellij.ide.presentation.Presentation;
import com.intellij.psi.PsiClass;
import com.intellij.util.xml.*;
import com.intellij.struts.dom.converters.StrutsBooleanConverter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * struts-config_1_3.dtd:message-resources interface.
 * Type message-resources documentation
 * <pre>
 *  The "message-resources" element describes a MessageResources object with
 *      message templates for this module. The following attributes are defined:
 *      className       The configuration bean for this message resources object.
 *                      If specified, the object must be a subclass of the default
 *                      configuration bean.
 *                      ["org.apache.struts.config.MessageResourcesConfig"]
 *      factory         Fully qualified Java class name of the
 *                      MessageResourcesFactory subclass to use for this message
 *                      resources object.
 *                      ["org.apache.struts.util.PropertyMessageResourcesFactory"]
 *      key             Servlet context attribute under which this message
 *                      resources bundle will be stored. The default attribute is
 *                      the value specified by the string constant at
 *                      [Globals.MESSAGES_KEY]. The module prefix (if
 *                      any) is appended to the key (${key}${prefix}).
 *                      [org.apache.struts.Globals.MESSAGES_KEY]
 *                      NOTE: The module  prefix includes the leading
 *                      slash, so the default message resource bundle for a module
 *                      named "foo" is stored under
 *                      "org.apache.struts.action.MESSAGE/foo".
 *      null            Set to "true" if you want our message resources to return a
 *                      null string for unknown message keys, or "false" to return a
 *                      message with the bad key value.
 *      parameter       Configuration parameter to be passed to the createResources
 *                      method of our factory object.
 * </pre>
 */
@Presentation(icon = "StrutsApiIcons.MessageResources")
public interface MessageResources extends StrutsRootElement {

  /**
   * Returns the value of the null child.
   * Attribute null
   *
   * @return the value of the null child.
   */
  @Convert(StrutsBooleanConverter.class)
  @NotNull
  GenericAttributeValue<Boolean> getNull();


  /**
   * Returns the value of the factory child.
   * Attribute factory
   *
   * @return the value of the factory child.
   */
  @ExtendClass("org.apache.struts.util.MessageResourcesFactory")
  @NotNull
  GenericAttributeValue<PsiClass> getFactory();


  /**
   * Returns the value of the key child.
   * Attribute key
   *
   * @return the value of the key child.
   */
  @NotNull
  GenericAttributeValue<String> getKey();


  /**
   * Returns the value of the className child.
   * Attribute className
   *
   * @return the value of the className child.
   */
  @ExtendClass("org.apache.struts.config.MessageResourcesConfig")
  @NotNull
  GenericAttributeValue<PsiClass> getClassName();


  /**
   * Returns the value of the parameter child.
   * Attribute parameter
   *
   * @return the value of the parameter child.
   */
  @NameValue(referencable = false)
  @Required
  @NotNull
  GenericAttributeValue<String> getParameter();


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
