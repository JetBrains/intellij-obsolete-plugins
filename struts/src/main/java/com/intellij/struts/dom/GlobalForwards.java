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
import com.intellij.util.xml.ExtendClass;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Stubbed;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * struts-config_1_3.dtd:global-forwards interface.
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
 */
@Presentation(icon = "StrutsApiIcons.GlobalForwards")
public interface GlobalForwards extends StrutsRootElement {

  @NonNls String FOLDER_ELEMENT = "forwards";

  /**
   * Returns the value of the type child.
   * Attribute type
   *
   * @return the value of the type child.
   */
  @ExtendClass("org.apache.struts.action.ActionForward")
  @NotNull
  GenericAttributeValue<PsiClass> getType();


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
