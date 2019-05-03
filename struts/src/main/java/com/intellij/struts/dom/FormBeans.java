/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

// Generated on Wed Apr 05 15:23:26 MSD 2006
// DTD/Schema  :    struts-config_1_3.dtd

package com.intellij.struts.dom;

import com.intellij.ide.presentation.Presentation;
import com.intellij.psi.PsiClass;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Stubbed;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * struts-config_1_3.dtd:form-beans interface.
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
 */
@Presentation(icon = "StrutsApiIcons.FormBeans")
public interface FormBeans extends StrutsRootElement {

  /**
   * Returns the value of the type child.
   * Attribute type
   *
   * @return the value of the type child.
   */
  @NotNull
  GenericAttributeValue<PsiClass> getType();


  /**
   * Returns the list of form-bean children.
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
   *
   * @return the list of form-bean children.
   */
  @Stubbed
  List<FormBean> getFormBeans();

  /**
   * Adds new child to the list of form-bean children.
   *
   * @return created child
   */
  FormBean addFormBean();


}
