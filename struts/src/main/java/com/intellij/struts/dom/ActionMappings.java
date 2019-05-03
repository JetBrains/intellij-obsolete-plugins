/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

// Generated on Wed Apr 05 15:23:26 MSD 2006
// DTD/Schema  :    struts-config_1_3.dtd

package com.intellij.struts.dom;

import com.intellij.ide.presentation.Presentation;
import com.intellij.psi.PsiClass;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.ExtendClass;
import com.intellij.util.xml.Stubbed;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * struts-config_1_3.dtd:action-mappings interface.
 * Type action-mappings documentation
 * <pre>
 *  The "action-mappings" element describes a set of ActionMapping objects
 *      [org.apache.struts.action.ActionMapping] that are available to process
 *      requests matching the url-pattern our ActionServlet registered with the
 *      container. The individual ActionMappings are configured through nested
 *      <action> elements. The following attributes are defined:
 *      type           Fully qualified Java class to use when instantiating
 *                     ActionMapping objects. If specified, the object must be a
 *                     subclass of the default class type.
 *                     WARNING:  For Struts 1.0, this value is ignored.  You
 *                     can set the default implementation class name with the
 *                     "mapping" initialization parameter to the Struts
 *                     controller servlet.
 * </pre>
 */
@Presentation(icon = "StrutsApiIcons.ActionMapping")
public interface ActionMappings extends StrutsRootElement {

  /**
   * Returns the value of the type child.
   * Attribute type
   *
   * @return the value of the type child.
   */
  @ExtendClass("org.apache.struts.action.ActionMapping")
  @NotNull
  GenericAttributeValue<PsiClass> getType();


  /**
   * Returns the list of action children.
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
   *
   * @return the list of action children.
   */
  @Stubbed
  List<Action> getActions();

  /**
   * Adds new child to the list of action children.
   *
   * @return created child
   */
  Action addAction();


}
