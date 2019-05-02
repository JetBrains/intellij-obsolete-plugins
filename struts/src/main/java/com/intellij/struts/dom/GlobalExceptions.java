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

// Generated on Wed Apr 05 15:29:47 MSD 2006
// DTD/Schema  :    struts-config_1_3.dtd

package com.intellij.struts.dom;


import com.intellij.ide.presentation.Presentation;
import org.jetbrains.annotations.NonNls;

import java.util.List;

/**
 * struts-config_1_3.dtd:global-exceptions interface.
 * Type global-exceptions documentation
 * <pre>
 *  The "global-exceptions" element describes a set of exceptions that might be
 *      thrown by an Action object. The handling of individual exception types is
 *      configured through nested exception elements. An <action> element may
 *      override a global exception handler by registering a local exception handler
 *      for the same exception type. Since Struts 1.1.
 * </pre>
 */
@Presentation(icon = "StrutsApiIcons.GlobalException")
public interface GlobalExceptions extends StrutsRootElement {

  @NonNls String FOLDER_ELEMENT = "global-exceptions";

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


}
