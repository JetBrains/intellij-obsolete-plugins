/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

// Generated on Wed Apr 05 15:29:47 MSD 2006
// DTD/Schema  :    tiles-config_1_3.dtd

package com.intellij.struts.dom.tiles;

import com.intellij.ide.presentation.Presentation;
import com.intellij.struts.dom.StrutsRootElement;
import com.intellij.util.xml.*;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;

/**
 * tiles-config_1_3.dtd:item interface.
 * Type item documentation
 * <pre>
 *  The "item" element describes an element of a list. It create a bean added as
 *      element to the list. Each bean can contain different properties: value, link,
 *      icon, tooltip. These properties are to be interpreted by the jsp page using
 *      them.
 *      By default the bean is of type
 *      "org.apache.struts.tiles.beans.SimpleMenuItem". This bean is useful to
 *      create a list of beans used as menu items.
 *      classtype       The fully qualified classtype for this bean.
 *                      If specified, the classtype must be a subclass of the interface
 *                      "org.apache.struts.tiles.beans.MenuItem".
 *      icon            The bean 'icon' property.
 *      link            The bean 'link' property.
 *      tooltip         The bean 'tooltip' property.
 *      value           The bean 'value' property.
 * </pre>
 */
@Presentation(icon = "StrutsApiIcons.Tiles.Item")
public interface Item extends StrutsRootElement {

  /**
   * Returns the value of the simple content.
   *
   * @return the value of the simple content.
   */
  @NotNull
  String getValue();

  /**
   * Sets the value of the simple content.
   *
   * @param value the new value to set
   */
  void setValue(@NotNull String value);


  /**
   * Returns the value of the icon child.
   * Attribute icon
   *
   * @return the value of the icon child.
   */
  @NotNull
  GenericAttributeValue<String> getIcon();


  /**
   * Returns the value of the link child.
   * Attribute link
   *
   * @return the value of the link child.
   */
  @Required
  @NotNull
  GenericAttributeValue<String> getLink();


  /**
   * Returns the value of the value child.
   * Attribute value
   *
   * @return the value of the value child.
   */
  @Required
  @Attribute("value")
  @NotNull
  GenericAttributeValue<String> getAttributeValue();


  /**
   * Returns the value of the tooltip child.
   * Attribute tooltip
   *
   * @return the value of the tooltip child.
   */
  @NotNull
  GenericAttributeValue<String> getTooltip();


  /**
   * Returns the value of the classtype child.
   * Attribute classtype
   *
   * @return the value of the classtype child.
   */
  @ExtendClass("org.apache.struts.tiles.beans.MenuItem")
  @NotNull
  GenericAttributeValue<PsiClass> getClasstype();


}
