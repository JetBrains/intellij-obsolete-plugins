/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
