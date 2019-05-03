/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

// Generated on Wed Apr 05 15:29:47 MSD 2006
// DTD/Schema  :    tiles-config_1_3.dtd

package com.intellij.struts.dom.tiles;

import com.intellij.ide.presentation.Presentation;
import com.intellij.psi.PsiClass;
import com.intellij.struts.dom.SetProperty;
import com.intellij.struts.dom.StrutsRootElement;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * tiles-config_1_3.dtd:bean interface.
 * Type bean documentation
 * <pre>
 *  The "bean" element describes an element of a list. It create a bean of the
 *      specified java classtype. This bean is initialized with appropriate nested
 *      <set-property>.
 *      classtype       The fully qualified classname for this bean.
 * </pre>
 */
@Presentation(icon = "StrutsApiIcons.FormBean")
public interface Bean extends StrutsRootElement {

  /**
   * Returns the value of the classtype child.
   * Attribute classtype
   *
   * @return the value of the classtype child.
   */
  @Required
  @NotNull
  GenericAttributeValue<PsiClass> getClasstype();


  /**
   * Returns the list of set-property children.
   * Type set-property documentation
   * <pre>
   *  The "set-property" element specifies the method name and initial value of
   *      a bean property. When the object representing
   *      the surrounding element is instantiated, the accessor for the indicated
   *      property is called and passed the indicated value.
   *      property        Name of the JavaBeans property whose setter method
   *                      will be called.
   *      value           String representation of the value to which this
   *                      property will be set, after suitable type conversion
   * </pre>
   *
   * @return the list of set-property children.
   */
  List<SetProperty> getSetProperties();

  /**
   * Adds new child to the list of set-property children.
   *
   * @return created child
   */
  SetProperty addSetProperty();


}
