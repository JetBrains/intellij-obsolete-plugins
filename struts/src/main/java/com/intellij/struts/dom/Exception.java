/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

// Generated on Wed Apr 05 15:23:26 MSD 2006
// DTD/Schema  :    struts-config_1_3.dtd

package com.intellij.struts.dom;

import com.intellij.ide.presentation.Presentation;
import com.intellij.openapi.paths.PathReference;
import com.intellij.psi.PsiClass;
import com.intellij.util.xml.*;
import com.intellij.struts.dom.converters.StrutsPathReferenceConverter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * struts-config_1_3.dtd:exception interface.
 * Type exception documentation
 * <pre>
 *  The "exception" element registers an ExceptionHandler for an exception type.
 *      The following attributes are defined:
 *     bundle           Servlet context attribute for the message resources bundle
 *                      associated with this handler. The default attribute is the
 *                      value specified by the string constant declared at
 *                      Globals.MESSAGES_KEY.
 *                      [org.apache.struts.Globals.MESSAGES_KEY]
 *     className        The configuration bean for this ExceptionHandler object.
 *                      If specified, className must be a subclass of the default
 *                      configuration bean
 *                      ["org.apache.struts.config.ExceptionConfig"]
 *     extends          The name of the exception handler that this
 *                      will inherit configuration information from.
 *     handler          Fully qualified Java class name for this exception handler.
 *                      ["org.apache.struts.action.ExceptionHandler"]
 *     key              The key to use with this handler's message resource bundle
 *                      that will retrieve the error message template for this
 *                      exception.
 *     path             The module-relative URI to the resource that will complete
 *                      the request/response if this exception occurs.
 *     scope            The context ("request" or "session") that is used to access
 *                      the ActionMessage object
 *                      [org.apache.struts.action.ActionMessage] for this
 *                      exception.
 *     type             Fully qualified Java class name of the exception type to
 *                      register with this handler.
 * </pre>
 */
@Presentation(icon = "StrutsApiIcons.GlobalException")
public interface Exception extends StrutsRootElement {

  @NonNls String EXCEPTION = "exception";

  /**
   * Returns the value of the extends child.
   * Attribute extends
   *
   * @return the value of the extends child.
   */
  @NotNull
  GenericAttributeValue<Exception> getExtends();


  /**
   * Returns the value of the className child.
   * Attribute className
   *
   * @return the value of the className child.
   */
  @ExtendClass("org.apache.struts.config.ExceptionConfig")
  @NotNull
  GenericAttributeValue<PsiClass> getClassName();


  /**
   * Returns the value of the type child.
   * Attribute type
   *
   * @return the value of the type child.
   */
  @ExtendClass(value = "java.lang.Exception", instantiatable = false)
  @Required
  @NotNull
  GenericAttributeValue<PsiClass> getType();


  /**
   * Returns the value of the key child.
   * Attribute key
   *
   * @return the value of the key child.
   */
  @NameValue(referencable = false, unique = false)
  @NotNull
  @Required
  GenericAttributeValue<String> getKey();


  /**
   * Returns the value of the handler child.
   * Attribute handler
   *
   * @return the value of the handler child.
   */
  @ExtendClass("org.apache.struts.action.ExceptionHandler")
  @NotNull
  GenericAttributeValue<PsiClass> getHandler();


  /**
   * Returns the value of the scope child.
   * Attribute scope
   *
   * @return the value of the scope child.
   */
  @NotNull
  GenericAttributeValue<RequestScope> getScope();


  /**
   * Returns the value of the path child.
   * Attribute path
   *
   * @return the value of the path child.
   */
  @Convert(StrutsPathReferenceConverter.class)
  @NotNull
  GenericAttributeValue<PathReference> getPath();


  /**
   * Returns the value of the bundle child.
   * Attribute bundle
   *
   * @return the value of the bundle child.
   */
  @NotNull
  GenericAttributeValue<String> getBundle();


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


}
