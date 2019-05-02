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
// DTD/Schema  :    validator_1_2_0.dtd

package com.intellij.struts.dom.validator;


import com.intellij.struts.dom.StrutsRootElement;
import com.intellij.ide.presentation.Presentation;
import org.jetbrains.annotations.NonNls;

import java.util.List;

/**
 * validator_1_2_0.dtd:form-validation interface.
 * Type form-validation documentation
 * <pre>
 *      The "form-validation" element is the root of the configuration file
 *      hierarchy, and contains nested elements for all of the other
 *      configuration settings.
 * </pre>
 */
@Presentation(icon = "StrutsApiIcons.Validator.ValidatorConfig")
public interface FormValidation extends StrutsRootElement {

  @NonNls String FORM_VALIDATION = "form-validation";

  /**
   * Returns the list of global children.
   * Type global documentation
   * <pre>
   *     The elements defined here are all global and must be nested within a
   *     "global" element.
   * </pre>
   *
   * @return the list of global children.
   */
  List<Global> getGlobals();

  /**
   * Adds new child to the list of global children.
   *
   * @return created child
   */
  Global addGlobal();


  /**
   * Returns the list of formset children.
   * Type formset documentation
   * <pre>
   *       The "formset" element defines a set of forms for a locale. Formsets for
   *       specific locales can override only those fields that change. The
   *       localization is properly scoped, so that a formset can override just the
   *       language, or just the country, or both.
   * </pre>
   *
   * @return the list of formset children.
   */
  List<Formset> getFormsets();

  /**
   * Adds new child to the list of formset children.
   *
   * @return created child
   */
  Formset addFormset();


}
