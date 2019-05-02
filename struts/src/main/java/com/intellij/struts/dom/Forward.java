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
import com.intellij.openapi.paths.PathReference;
import com.intellij.psi.PsiClass;
import com.intellij.struts.dom.converters.ForwardNameConverter;
import com.intellij.struts.dom.converters.StrutsBooleanConverter;
import com.intellij.struts.dom.converters.StrutsPathReferenceConverter;
import com.intellij.util.xml.*;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * struts-config_1_3.dtd:forward interface.
 * Type forward documentation
 * <pre>
 *  The "forward" element describes an ActionForward that is to be made
 *      available to an Action as a return value. An ActionForward is referenced by
 *      a logical name and encapsulates a URI. A "forward" element may be used to
 *      describe both global and local ActionForwards. Global forwards are available
 *      to all the Action objects in the module. Local forwards can be
 *      nested within an <action> element and only available to an Action object
 *      when it is invoked through that ActionMapping.
 *      className       Fully qualified Java class name of ActionForward
 *                      subclass to use for this object.
 *                      ["org.apache.struts.action.ActionForward"]
 *      extends         The name of the forward configuration that this
 *                      will inherit configuration information from.
 *      module          The module prefix to use with this path. This value should
 *                      begin with a slash ("/").
 *      name            The unique identifier for this forward. Referenced by the
 *                      Action object at runtime to select - by its logical name -
 *                      the resource that should complete the request/response.
 *      path            The module-relative path to the resources that is
 *                      encapsulated by the logical name of this ActionForward.
 *                      This value should begin with a slash ("/") character.
 *      redirect        Set to "true" if a redirect instruction should be issued to
 *                      the user-agent so that a new request is issued for this
 *                      forward's resource. If true,  RequestDispatcher.Redirect is
 *                      called. If "false", RequestDispatcher.forward is called instead.
 *                      [false]
 * </pre>
 */
@Presentation(icon = "StrutsApiIcons.Forward")
public interface Forward extends StrutsRootElement {

  @NonNls String FORWARD = "forward";

  /**
   * Returns the value of the name child.
   * Attribute name
   *
   * @return the value of the name child.
   */
  @NotNull
  @Stubbed
  @NameValue
  @Required
  @Referencing(ForwardNameConverter.class)
  GenericAttributeValue<String> getName();


  /**
   * Returns the value of the module child.
   * Attribute module
   *
   * @return the value of the module child.
   */
  @Attribute("module")
  @NotNull
  GenericAttributeValue<String> getStrutsModule();


  /**
   * Returns the value of the command child.
   * Attribute command
   *
   * @return the value of the command child.
   */
  @NotNull
  GenericAttributeValue<String> getCommand();


  /**
   * Returns the value of the extends child.
   * Attribute extends
   *
   * @return the value of the extends child.
   */
  @NotNull
  GenericAttributeValue<Forward> getExtends();


  /**
   * Returns the value of the catalog child.
   * Attribute catalog
   *
   * @return the value of the catalog child.
   */
  @NotNull
  GenericAttributeValue<String> getCatalog();


  /**
   * Returns the value of the redirect child.
   * Attribute redirect
   *
   * @return the value of the redirect child.
   */
  @NotNull
  @Convert(StrutsBooleanConverter.class)
  GenericAttributeValue<Boolean> getRedirect();


  /**
   * Returns the value of the className child.
   * Attribute className
   *
   * @return the value of the className child.
   */
  @Stubbed
  @ExtendClass("org.apache.struts.action.ActionForward")
  @NotNull
  GenericAttributeValue<PsiClass> getClassName();


  /**
   * Returns the value of the path child.
   * Attribute path
   *
   * @return the value of the path child.
   */
  @Stubbed
  @Convert(StrutsPathReferenceConverter.class)
  @Required
  @NotNull
  GenericAttributeValue<PathReference> getPath();


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
