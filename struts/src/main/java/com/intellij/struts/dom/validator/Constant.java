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
 * validator_1_2_0.dtd:constant interface.
 * Type constant documentation
 * <pre>
 *      The "constant" element defines a static value that can be used as
 *      replacement parameters within "field" elements. The "constant-name" and
 *      "constant-value" elements define the constant's reference id and replacement
 *      value.
 * </pre>
 */
@Presentation(icon = "StrutsApiIcons.Validator.Constant")
public interface Constant extends StrutsRootElement {

  /**
   * Returns the value of the constant-name child.
   *
   * @return the value of the constant-name child.
   */
  @NotNull
  @NameValue
  GenericDomValue<String> getConstantName();


  /**
   * Returns the value of the constant-value child.
   *
   * @return the value of the constant-value child.
   */
  @NotNull
  GenericDomValue<String> getConstantValue();


}
