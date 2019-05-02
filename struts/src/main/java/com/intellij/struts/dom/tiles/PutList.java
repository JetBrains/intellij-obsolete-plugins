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
// DTD/Schema  :    tiles-config_1_3.dtd

package com.intellij.struts.dom.tiles;

import com.intellij.ide.presentation.Presentation;
import com.intellij.struts.dom.StrutsRootElement;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.NameValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * tiles-config_1_3.dtd:putList interface.
 * Type putList documentation
 * <pre>
 *  The "putList" element describes a list attribute of a definition. It allows to
 *      specify an attribute that is a java List containing any kind of values. In
 *      the config file, the list elements are specified by nested <add>, <item> or
 *      <putList>.
 *      name            The unique identifier for this put list.
 * </pre>
 */
@Presentation(icon = "StrutsApiIcons.Tiles.PutList")
public interface PutList extends StrutsRootElement {

  /**
   * Returns the value of the name child.
   * Attribute name
   *
   * @return the value of the name child.
   */
  @NameValue
  @Required
  @NotNull
  GenericAttributeValue<String> getName();


  /**
   * Returns the list of add children.
   * Type add documentation
   * <pre>
   *  The "add" element describes an element of a list. It is similar to the
   *      <put> element.
   *      content         Same as value. For compatibility with the template tag library.
   *      direct          Same as type="string". For compatibility with the template
   *                      tag library.
   *      type            The type of the value. Can be: string, page, template or definition.
   *                      By default, no type is associated to a value. If a type is
   *                      associated, it will be used as a hint to process the value
   *                      when the attribute will be used in the inserted tiles.
   *      value           The value associated to this tiles attribute. The value should
   *                      be specified with this tag attribute, or in the body of the tag.
   * </pre>
   *
   * @return the list of add children.
   */
  List<Add> getAdds();

  /**
   * Adds new child to the list of add children.
   *
   * @return created child
   */
  Add addAdd();


  /**
   * Returns the list of item children.
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
   *
   * @return the list of item children.
   */
  List<Item> getItems();

  /**
   * Adds new child to the list of item children.
   *
   * @return created child
   */
  Item addItem();


  /**
   * Returns the list of bean children.
   * Type bean documentation
   * <pre>
   *  The "bean" element describes an element of a list. It create a bean of the
   *      specified java classtype. This bean is initialized with appropriate nested
   *      <set-property>.
   *      classtype       The fully qualified classname for this bean.
   * </pre>
   *
   * @return the list of bean children.
   */
  List<Bean> getBeans();

  /**
   * Adds new child to the list of bean children.
   *
   * @return created child
   */
  Bean addBean();


  /**
   * Returns the list of putList children.
   * Type putList documentation
   * <pre>
   *  The "putList" element describes a list attribute of a definition. It allows to
   *      specify an attribute that is a java List containing any kind of values. In
   *      the config file, the list elements are specified by nested <add>, <item> or
   *      <putList>.
   *      name            The unique identifier for this put list.
   * </pre>
   *
   * @return the list of putList children.
   */
  List<PutList> getPutLists();

  /**
   * Adds new child to the list of putList children.
   *
   * @return created child
   */
  PutList addPutList();


}
