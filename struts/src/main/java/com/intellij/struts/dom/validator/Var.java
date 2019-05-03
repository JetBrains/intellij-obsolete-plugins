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
