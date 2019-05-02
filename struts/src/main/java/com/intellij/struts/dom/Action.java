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
import com.intellij.struts.dom.converters.FormBeanScopeProvider;
import com.intellij.struts.dom.converters.NameConverter;
import com.intellij.struts.dom.converters.StrutsBooleanConverter;
import com.intellij.struts.dom.converters.StrutsPathReferenceConverter;
import com.intellij.util.xml.*;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * struts-config_1_3.dtd:action interface.
 * Type action documentation
 * <pre>
 *  The "action" element describes an ActionMapping object that is to be used
 *      to process a request for a specific module-relative URI. The following
 *      attributes are defined:
 *      attribute       Name of the request-scope or session-scope attribute that
 *                      is used to access our ActionForm bean, if it is other than
 *                      the bean's specified "name". Optional if "name" is specified,
 *                      else not valid.
 *      catalog         The name of a commons-chain catalog in which to look up
 *                      a command to be executed as part of servicing this request.
 *                      Only meaningful if "command" is also specified.
 *      className       The fully qualified Java class name of the ActionMapping
 *                      subclass to use for this action mapping object. Defaults to
 *                      the type specified by the enclosing <action-mappings>
 *                      element or to "org.apache.struts.action.ActionMapping" if
 *                      not specified.
 *                      ["org.apache.struts.action.ActionMapping"]
 *      command         The name of a commons-chain command which should be looked up
 *                      and executed as part of servicing this request.
 *      extends         The path of the action mapping configuration that this
 *                      will inherit configuration information from.
 *      forward         Module-relative path of the servlet or other resource that
 *                      will process this request, instead of the Action class
 *                      specified by "type".  The path WILL NOT be processed
 *                      through the "forwardPattern" attribute that is configured
 *                      on the "controller" element for this module.
 *                      Exactly one of "forward", "include", or "type" must be
 *                      specified.
 *      include         Module-relative path of the servlet or other resource that
 *                      will process this request, instead of the Action class
 *                      specified by "type".  The path WILL NOT be processed
 *                      through the "forwardPattern" attribute that is configured
 *                      on the "controller" element for this module.
 *                      Exactly one of "forward", "include", or "type" must be
 *                      specified.
 *      input           Module-relative path of the action or other resource to
 *                      which control should be returned if a validation error is
 *                      encountered. Valid only when "name" is specified. Required
 *                      if "name" is specified and the input bean returns
 *                      validation errors. Optional if "name" is specified and the
 *                      input bean does not return validation errors.
 *      name            Name of the form bean, if any, that is associated with this
 *                      action mapping.
 *      path            The module-relative path of the submitted request, starting
 *                      with a "/" character, and without the filename extension if
 *                      extension mapping is used.
 *                      NOTE:  Do *not* include a period in your path name,
 *                      because it will look like a filename extension and
 *                      cause your Action to not be located.
 *      parameter       General-purpose configuration parameter that can be used to
 *                      pass extra information to the Action object selected by
 *                      this action mapping.
 *      prefix          Prefix used to match request parameter names to ActionForm
 *                      property names, if any. Optional if "name" is specified,
 *                      else not allowed.
 *      roles           Comma-delimited list of security role names that are allowed
 *                      access to this ActionMapping object. Since Struts 1.1.
 *      scope           The context ("request" or "session") that is used to
 *                      access our ActionForm bean, if any.  Optional if "name" is
 *                      specified, else not valid. [session]
 *      suffix          Suffix used to match request parameter names to ActionForm
 *                      bean property names, if any. Optional if "name" is
 *                      specified, else not valid.
 *      type            Fully qualified Java class name of the Action subclass
 *                      [org.apache.struts.action.Action] that will process requests
 *                      for this action mapping. Not valid if either the "forward"
 *                      or "include" attribute is specified.  Exactly one of
 *                      "forward", "include", or "type" must be specified.
 *      unknown         Set to "true" if this object should be configured as the
 *                      default action mapping for this module. If a request does not
 *                      match another object, it will be passed to the ActionMapping
 *                      object with unknown set to "true". Only one ActionMapping
 *                      can be marked as "unknown" within a module.
 *                      [false]
 *      validate        Set to "true" if the validate method of the ActionForm bean
 *                      should be called prior to calling the Action object for this
 *                      action mapping, or set to "false" if you do not want the
 *                      validate method called.
 *                      [true]
 * </pre>
 */
@Presentation(icon = "StrutsApiIcons.ActionMapping")
public interface Action extends StrutsRootElement {

  @NonNls String ACTION = "action";
  @NonNls String INPUT = "input";

  /**
   * Returns the value of the include child.
   * Attribute include
   *
   * @return the value of the include child.
   */
  @Convert(StrutsPathReferenceConverter.class)
  @NotNull
  GenericAttributeValue<PathReference> getInclude();


  /**
   * Returns the value of the scope child.
   * Attribute scope
   *
   * @return the value of the scope child.
   */
  @NotNull
  GenericAttributeValue<RequestScope> getScope();


  /**
   * Returns the value of the suffix child.
   * Attribute suffix
   *
   * @return the value of the suffix child.
   */
  @NotNull
  GenericAttributeValue<String> getSuffix();


  /**
   * Returns the value of the className child.
   * Attribute className
   *
   * @return the value of the className child.
   */
  @NotNull
  @ExtendClass("org.apache.struts.action.ActionMapping")
  GenericAttributeValue<PsiClass> getClassName();


  /**
   * Returns the value of the type child.
   * Attribute type
   *
   * @return the value of the type child.
   */
  @Stubbed
  @NotNull
  @ExtendClass("org.apache.struts.action.Action")
  GenericAttributeValue<PsiClass> getType();


  /**
   * Returns the value of the command child.
   * Attribute command
   *
   * @return the value of the command child.
   */
  @NotNull
  GenericAttributeValue<String> getCommand();


  /**
   * Returns the value of the parameter child.
   * Attribute parameter
   *
   * @return the value of the parameter child.
   */
  @NotNull
  @Stubbed
  GenericAttributeValue<String> getParameter();


  /**
   * Returns the value of the catalog child.
   * Attribute catalog
   *
   * @return the value of the catalog child.
   */
  @NotNull
  GenericAttributeValue<String> getCatalog();


  /**
   * Returns the value of the extends child.
   * Attribute extends
   *
   * @return the value of the extends child.
   */
  @NotNull
  GenericAttributeValue<Action> getExtends();


  /**
   * Returns the value of the forward child.
   * Attribute forward
   *
   * @return the value of the forward child.
   */
  @Convert(StrutsPathReferenceConverter.class)
  @NotNull
  GenericAttributeValue<PathReference> getForward();


  /**
   * Returns the value of the input child.
   * Attribute input
   *
   * @return the value of the input child.
   */
  @Convert(StrutsPathReferenceConverter.class)
  @NotNull
  GenericAttributeValue<PathReference> getInput();


  /**
   * Returns the value of the attribute child.
   * Attribute attribute
   *
   * @return the value of the attribute child.
   */
  @NotNull
  GenericAttributeValue<String> getAttribute();


  /**
   * Returns the value of the roles child.
   * Attribute roles
   *
   * @return the value of the roles child.
   */
  @NotNull
  GenericAttributeValue<String> getRoles();


  /**
   * Returns the value of the validate child.
   * Attribute validate
   *
   * @return the value of the validate child.
   */
  @NotNull
  @Convert(StrutsBooleanConverter.class)
  GenericAttributeValue<Boolean> getValidate();


  /**
   * Returns the value of the path child.
   * Attribute path
   *
   * @return the value of the path child.
   */
  @Stubbed
  @NotNull
  @NameValue
  @Required
  @Referencing(NameConverter.ForAction.class)
  GenericAttributeValue<String> getPath();


  /**
   * Returns the value of the prefix child.
   * Attribute prefix
   *
   * @return the value of the prefix child.
   */
  @NotNull
  GenericAttributeValue<String> getPrefix();


  /**
   * Returns the value of the unknown child.
   * Attribute unknown
   *
   * @return the value of the unknown child.
   */
  @NotNull
  @Convert(StrutsBooleanConverter.class)
  GenericAttributeValue<Boolean> getUnknown();


  /**
   * Returns the value of the name child.
   * Attribute name
   *
   * @return the value of the name child.
   */
  @Stubbed
  @Scope(FormBeanScopeProvider.class)
  @NotNull
  GenericAttributeValue<FormBean> getName();


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
   * Returns the list of exception children.
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
   *
   * @return the list of exception children.
   */
  List<Exception> getExceptions();

  /**
   * Adds new child to the list of exception children.
   *
   * @return created child
   */
  Exception addException();


  /**
   * Returns the list of forward children.
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
   *
   * @return the list of forward children.
   */
  @Stubbed
  List<Forward> getForwards();

  /**
   * Adds new child to the list of forward children.
   *
   * @return created child
   */
  Forward addForward();


}
