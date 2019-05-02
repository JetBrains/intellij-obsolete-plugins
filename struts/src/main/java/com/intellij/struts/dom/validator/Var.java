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
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.NameValue;
import org.jetbrains.annotations.NotNull;

/**
 * validator_1_2_0.dtd:var interface.
 * Type var documentation
 * <pre>
 *      The "var" element can set parameters that a field may need to pass to
 *      one of its validators, such as the minimum and maximum values in a
 *      range validation. These parameters may also be referenced by one of the
 *      arg? elements using a shell syntax: ${var:var-name}.
 * </pre>
 */
@Presentation(icon = "AllIcons.Nodes.Variable")
public interface Var extends StrutsRootElement {

  /**
   * Returns the value of the var-name child.
   * Type var-name documentation
   * <pre>
   *      The name of the var parameter to provide to a field's validators.
   * </pre>
   *
   * @return the value of the var-name child.
   */
  @NotNull
  @NameValue
  GenericDomValue<String> getVarName();


  /**
   * Returns the value of the var-value child.
   * Type var-value documentation
   * <pre>
   *      The value of the var parameter to provide to a field's validators.
   * </pre>
   *
   * @return the value of the var-value child.
   */
  @NotNull
  GenericDomValue<String> getVarValue();


  /**
   * Returns the value of the var-jstype child.
   * Type var-jstype documentation
   * <pre>
   *      The java script type, Possible Values [int| string | regexp]
   * </pre>
   *
   * @return the value of the var-jstype child.
   */
  GenericDomValue<String> getVarJstype();


}
