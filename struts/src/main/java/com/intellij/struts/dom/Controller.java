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
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.ExtendClass;
import com.intellij.struts.dom.converters.MemSizeConverter;
import com.intellij.struts.dom.converters.StrutsBooleanConverter;
import com.intellij.util.xml.Stubbed;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * struts-config_1_3.dtd:controller interface.
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
 */
@Presentation(icon = "StrutsApiIcons.Controller")
public interface Controller extends StrutsRootElement {

  /**
   * Returns the value of the memFileSize child.
   * Attribute memFileSize
   *
   * @return the value of the memFileSize child.
   */
  @Convert(MemSizeConverter.class)
  @NotNull
  GenericAttributeValue<Long> getMemFileSize();


  /**
   * Returns the value of the bufferSize child.
   * Attribute bufferSize
   *
   * @return the value of the bufferSize child.
   */
  @NotNull
  GenericAttributeValue<Integer> getBufferSize();


  /**
   * Returns the value of the inputForward child.
   * Attribute inputForward
   *
   * @return the value of the inputForward child.
   */
  @Stubbed
  @Convert(StrutsBooleanConverter.class)
  @NotNull
  GenericAttributeValue<Boolean> getInputForward();


  /**
   * Returns the value of the maxFileSize child.
   * Attribute maxFileSize
   *
   * @return the value of the maxFileSize child.
   */
  @Convert(MemSizeConverter.class)
  @NotNull
  GenericAttributeValue<Long> getMaxFileSize();


  /**
   * Returns the value of the command child.
   * Attribute command
   *
   * @return the value of the command child.
   */
  @NotNull
  GenericAttributeValue<String> getCommand();


  /**
   * Returns the value of the pagePattern child.
   * Attribute pagePattern
   *
   * @return the value of the pagePattern child.
   */
  @NotNull
  GenericAttributeValue<String> getPagePattern();


  /**
   * Returns the value of the multipartClass child.
   * Attribute multipartClass
   *
   * @return the value of the multipartClass child.
   */
  @ExtendClass("org.apache.struts.upload.CommonsMultipartRequestHandler")
  @NotNull
  GenericAttributeValue<PsiClass> getMultipartClass();


  /**
   * Returns the value of the processorClass child.
   * Attribute processorClass
   *
   * @return the value of the processorClass child.
   */
  @ExtendClass("org.apache.struts.action.RequestProcessor")
  @NotNull
  GenericAttributeValue<PsiClass> getProcessorClass();


  /**
   * Returns the value of the catalog child.
   * Attribute catalog
   *
   * @return the value of the catalog child.
   */
  @NotNull
  GenericAttributeValue<String> getCatalog();


  /**
   * Returns the value of the tempDir child.
   * Attribute tempDir
   *
   * @return the value of the tempDir child.
   */
  @NotNull
  GenericAttributeValue<String> getTempDir();


  /**
   * Returns the value of the nocache child.
   * Attribute nocache
   *
   * @return the value of the nocache child.
   */
  @Convert(StrutsBooleanConverter.class)
  @NotNull
  GenericAttributeValue<Boolean> getNocache();


  /**
   * Returns the value of the className child.
   * Attribute className
   *
   * @return the value of the className child.
   */
  @ExtendClass("org.apache.struts.config.ControllerConfig")
  @NotNull
  GenericAttributeValue<PsiClass> getClassName();


  /**
   * Returns the value of the locale child.
   * Attribute locale
   *
   * @return the value of the locale child.
   */
  @NotNull
  GenericAttributeValue<String> getLocale();


  /**
   * Returns the value of the contentType child.
   * Attribute contentType
   *
   * @return the value of the contentType child.
   */
  @NotNull
  GenericAttributeValue<String> getContentType();


  /**
   * Returns the value of the forwardPattern child.
   * Attribute forwardPattern
   *
   * @return the value of the forwardPattern child.
   */
  @NotNull
  GenericAttributeValue<String> getForwardPattern();


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
  @Stubbed
  List<SetProperty> getSetProperties();

  /**
   * Adds new child to the list of set-property children.
   *
   * @return created child
   */
  SetProperty addSetProperty();


}
