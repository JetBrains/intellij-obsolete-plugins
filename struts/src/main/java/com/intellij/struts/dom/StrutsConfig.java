/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

// Generated on Wed Apr 05 15:23:26 MSD 2006
// DTD/Schema  :    struts-config_1_3.dtd

package com.intellij.struts.dom;

import com.intellij.ide.presentation.Presentation;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.Stubbed;
import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * struts-config_1_3.dtd:struts-config interface.
 * Type struts-config documentation
 * <pre>
 *  The "struts-config" element is the root of the configuration file
 *      hierarchy, and contains nested elements for all of the other
 *      configuration settings.
 * </pre>
 */
@Stubbed
@Presentation(icon = "StrutsApiIcons.StrutsConfig")
public interface StrutsConfig extends StrutsRootElement {

  @NonNls String STRUTS_CONFIG = "struts-config";

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
  @NotNull
  GenericDomValue<String> getDescription();

  @NotNull
  DataSources getDataSources();
  /**
   * Returns the value of the form-beans child.
   * Type form-beans documentation
   * <pre>
   *  The "form-beans" element describes the set of form bean descriptors for this
   *      module. The following attributes are defined:
   *      type            Fully qualified Java class to use when instantiating
   *                      ActionFormBean objects. If specified, the object must be a
   *                      subclass of the default class type.
   *                      WARNING:  For Struts 1.0, this value is ignored.  You
   *                      can set the default implementation class name with the
   *                      "formBean" initialization parameter to the Struts
   *                      controller servlet.
   * </pre>
   *
   * @return the value of the form-beans child.
   */
  @Stubbed
  @NotNull
  FormBeans getFormBeans();


  /**
   * Returns the value of the global-exceptions child.
   * Type global-exceptions documentation
   * <pre>
   *  The "global-exceptions" element describes a set of exceptions that might be
   *      thrown by an Action object. The handling of individual exception types is
   *      configured through nested exception elements. An <action> element may
   *      override a global exception handler by registering a local exception handler
   *      for the same exception type. Since Struts 1.1.
   * </pre>
   *
   * @return the value of the global-exceptions child.
   */
  @NotNull
  GlobalExceptions getGlobalExceptions();


  /**
   * Returns the value of the global-forwards child.
   * Type global-forwards documentation
   * <pre>
   *  The "global-forwards" element describes a set of ActionForward objects
   *      [org.apache.struts.action.ActionForward] that are available to all Action
   *      objects as a return value. The individual ActionForwards are configured
   *      through nested <forward> elements. An <action> element may override a global
   *      forward by defining a local <forward> of the same name.
   *      type            Fully qualified Java class to use when instantiating
   *                      ActionForward objects.  If specified, the object must be a
   *                      subclass of the default class type.
   *                      WARNING:  For Struts 1.0, this value is ignored.  You
   *                      can set the default implementation class name with the
   *                      "forward" initialization parameter to the Struts
   *                      controller servlet.
   * </pre>
   *
   * @return the value of the global-forwards child.
   */
  @Stubbed
  @NotNull
  GlobalForwards getGlobalForwards();


  /**
   * Returns the value of the action-mappings child.
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
   *
   * @return the value of the action-mappings child.
   */
  @Stubbed
  @NotNull
  ActionMappings getActionMappings();


  /**
   * Returns the value of the controller child.
   * Type controller documentation
   * <pre>
   *  The "controller" element describes the ControllerConfig bean
   *      [org.apache.struts.config.ControllerConfig] that encapsulates
   *      a module's runtime configuration. The following
   *      attributes are defined:
   *      bufferSize      The size of the input buffer used when processing
   *                      file uploads.
   *                      [4096]
   *      className       Fully qualified Java class name of the
   *                      ControllerConfig subclass for this controller object.
   *                      If specified, the object must be a subclass of the
   *                      default class.
   *                      ["org.apache.struts.config.ControllerConfig"]
   *      contentType     Default content type (and optional character encoding) to
   *                      be set on each response. May be overridden by the Action,
   *                      JSP, or other resource to which the request is forwarded.
   *                      ["text/html"]
   *      forwardPattern  Replacement pattern defining how the "path" attribute of a
   *                      <forward> element is mapped to a context-relative URL when
   *                      it starts with a slash (and when the contextRelative
   *                      property is false). This value may consist of any
   *                      combination of the following:
   *                      - "$M" - Replaced by the module prefix of this module
   *                      - "$P" - Replaced by the "path" attribute of the  selected
   *                      "forward" element
   *                      - "$$" - Causes a literal dollar sign to be rendered
   *                      - "$x" - (Where "x" is any character not defined above)
   *                      Silently swallowed, reserved for future use
   *                      If not specified, the default forwardPattern is "$M$P",
   *                      which is consistent with the previous behavior of
   *                      forwards.  Since Struts 1.1.  ["$M$P"]
   *      inputForward    Set to "true" if you want the "input" attribute of
   *                      <action> elements to be the name of a local or global
   *                      ActionForward, which will then be used to calculate the
   *                      ultimate URL. Set to "false" (the default) to treat the
   *                      "input" parameter of <action> elements as a
   *                      module-relative path to the resource
   *                      to be used as the input form. Since Struts 1.1.
   *                      [false]
   *      locale          Set to "true" if you want a Locale object stored in the
   *                      user's session if not already present.
   *                      [true]
   *      maxFileSize     The maximum size (in bytes) of a file to be accepted as a
   *                      file upload.  Can be expressed as a number followed by a
   *                      "K", "M", or "G", which are interpreted to mean kilobytes,
   *                      megabytes, or gigabytes, respectively.
   *                      ["250M"]
   *      memFileSize     The maximum size (in bytes) of a file whose contents will
   *                      be retained in memory after uploading. Files larger than
   *                      this threshold will be written to some alternative storage
   *                      medium, typically a hard disk. Can be expressed as a number
   *                      followed by a "K", "M", or "G", which are interpreted to
   *                      mean kilobytes, megabytes, or gigabytes, respectively.
   *                      ["256K"]
   *      multipartClass  The fully qualified Java class name of the multipart
   *                      request handler class to be used with this module.
   *                      ["org.apache.struts.upload.CommonsMultipartRequestHandler"]
   *      nocache         Set to "true" if you want the controller to add HTTP
   *                      headers for defeating caching to every response from
   *                      this module.  [false]
   *      pagePattern     Replacement pattern defining how the "page" attribute of
   *                      custom tags using it is mapped to a context-relative URL
   *                      of the corresponding resource.  This value may consist of
   *                      any combination of the following:
   *                      - "$M" - Replaced by the module prefix of this module
   *                      - "$P" - Replaced by the value of the "page" attribute
   *                      - "$$" - Causes a literal dollar sign to be rendered
   *                      - "$x" - (Where "x" is any character not defined above)
   *                               Silently swallowed, reserved for future use
   *                      If not specified, the default forwardPattern is
   *                      "$M$P", which is consistent with previous hard coded
   *                      behavior of URL evaluation for "page" attributes.
   *                      ["$M$P"]
   *      processorClass  The fully qualified Java class name of the
   *                      RequestProcessor subclass to be used with this module.
   *                      ["org.apache.struts.action.RequestProcessor"]
   *      tempDir         Temporary working directory to use when processing
   *                      file uploads.
   *                      [{Directory provided by servlet container}]
   *      catalog         Name of the catalog to use when processing requests
   *                      for this module.
   *                      [struts]
   *      command         Name of the command to execute to process a request.
   *                      [servlet-standard]
   * </pre>
   *
   * @return the value of the controller child.
   */
  @NotNull
  @Stubbed
  Controller getController();


  /**
   * Returns the list of message-resources children.
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
   *
   * @return the list of message-resources children.
   */
  @NotNull
  @SubTagList("message-resources")
  List<MessageResources> getMessageResources();

  /**
   * Adds new child to the list of message-resources children.
   *
   * @return created child
   */
  MessageResources addMessageResources();


  /**
   * Returns the list of plug-in children.
   * Type plug-in documentation
   * <pre>
   *  The "plug-in" element specifies the fully qualified class name of a
   *      general-purpose application plug-in module that receives notification of
   *      application startup and shutdown events. An instance of the specified class
   *      is created for each element, and can be configured with nested <set-property>
   *      elements. The following attributes are supported:
   *      className       Fully qualified Java class name of the plug-in class; must
   *                      implement [org.apache.struts.action.PlugIn].
   * </pre>
   *
   * @return the list of plug-in children.
   */
  @NotNull
  @Stubbed
  List<PlugIn> getPlugIns();

  /**
   * Adds new child to the list of plug-in children.
   *
   * @return created child
   */
  PlugIn addPlugIn();


}
