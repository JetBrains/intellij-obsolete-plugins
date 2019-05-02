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
// DTD/Schema  :    validator_1_2_0.dtd

package com.intellij.struts.dom.validator;

import com.intellij.ide.presentation.Presentation;
import com.intellij.struts.dom.StrutsRootElement;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.NameValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * validator_1_2_0.dtd:form interface.
 * Type form documentation
 * <pre>
 *      The "form" element defines a set of fields to be validated. The name
 *      corresponds to the identifier the application assigns to the form.
 * </pre>
 */
@Presentation(icon = "StrutsApiIcons.Validator.Form")
public interface Form extends StrutsRootElement {

  /**
   * Returns the value of the name child.
   * Attribute name
   *
   * @return the value of the name child.
   */
  @NameValue(referencable = false)
  @Required
  @NotNull
  GenericAttributeValue<String> getName();


  /**
   * Returns the value of the extends child.
   * Attribute extends
   *
   * @return the value of the extends child.
   */
  @NotNull
  GenericAttributeValue<String> getExtends();


  /**
   * Returns the list of field children.
   * Type field documentation
   * <pre>
   *      The "field" element defines the properties to be validated. In a
   *      web application, a field would also correspond to a control on
   *      a HTML form. To validate the properties, the validator works through
   *      a JavaBean representation. The field element accepts these
   *      attributes:
   *      property        The property on the JavaBean corresponding to this
   *                      field element.
   *      depends         The comma-delimited list of validators to apply against
   *                      this field. For the field to succeed, all the
   *                      validators must succeed.
   *      page            The JavaBean corresponding to this form may include
   *                      a page property. Only fields with a "page" attribute
   *                      value that is equal to or less than the page property
   *                      on the form JavaBean are processed. This is useful when
   *                      using a "wizard" approach to completing a large form,
   *                      to ensure that a page is not skipped.
   *                      [0]
   *      indexedListProperty
   *                      The "indexedListProperty" is the method name that will
   *                      return an array or a Collection used to retrieve the
   *                      list and then loop through the list performing the
   *                      validations for this field.
   * </pre>
   *
   * @return the list of field children.
   */
  @NotNull
  List<Field> getFields();

  /**
   * Adds new child to the list of field children.
   *
   * @return created child
   */
  Field addField();


}
