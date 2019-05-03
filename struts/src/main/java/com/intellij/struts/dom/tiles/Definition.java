/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

// Generated on Wed Apr 05 15:29:47 MSD 2006
// DTD/Schema  :    tiles-config_1_3.dtd

package com.intellij.struts.dom.tiles;

import com.intellij.ide.presentation.Presentation;
import com.intellij.javaee.model.xml.SecurityRole;
import com.intellij.openapi.paths.PathReference;
import com.intellij.psi.PsiClass;
import com.intellij.struts.SecurityRoleScopeProvider;
import com.intellij.struts.dom.Icon;
import com.intellij.struts.dom.StrutsRootElement;
import com.intellij.tiles.TilesConstants;
import com.intellij.util.xml.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * tiles-config_1_3.dtd:definition interface.
 * Type definition documentation
 * <pre>
 *  The "definition" element describes a definition that can be inserted in a jsp
 *      page. This definition is identified by its logical name. A definition allows
 *      to define all the attributes that can be set in <insert> tag from a jsp page.
 *      controllerClass The fully qualified Java class name of the controller
 *                      subclass to call immediately before the tiles is inserted.
 *                      Only one of controllerClass or controllerUrl should be
 *                      specified.
 *      controllerUrl   The context-relative path to the resource used as controller
 *                      called immediately before the tiles is inserted.
 *                      Only one of controllerClass or controllerUrl should be
 *                      specified.
 *      extends         Name of a definition that is used as ancestor of this definition.
 *                      All attributes from the ancestor are available to the new
 *                      definition. Any attribute inherited from the ancestor can
 *                      be overloaded by providing a new value.
 *      name            The unique identifier for this definition.
 *      page            Same as path.
 *      path            The context-relative path to the resource used as tiles to
 *                      insert. This tiles will be inserted and a tiles context
 *                      containing appropriate attributes will be available.
 *      role            Security role name that is allowed access to this definition
 *                      object. The definition is inserted only if the role name is
 *                      allowed.
 *      template        Same as path. For compatibility with the template tag library.
 * </pre>
 */
@Presentation(icon = "StrutsApiIcons.Tiles.Tile")
@Stubbed
public interface Definition extends StrutsRootElement {

  /**
   * Returns the value of the name child.
   * Attribute name
   *
   * @return the value of the name child.
   */
  @NotNull
  @NameValue
  @Stubbed
  GenericAttributeValue<String> getName();


  /**
   * Returns the value of the extends child.
   * Attribute extends
   *
   * @return the value of the extends child.
   */
  @NotNull
  @Stubbed
  GenericAttributeValue<Definition> getExtends();


  /**
   * Returns the value of the role child.
   * Attribute role
   *
   * @return the value of the role child.
   */
  @NotNull
  @Scope(SecurityRoleScopeProvider.class)
  GenericAttributeValue<SecurityRole> getRole();


  /**
   * Returns the value of the controllerUrl child.
   * Attribute controllerUrl
   *
   * @return the value of the controllerUrl child.
   */
  @NotNull
  GenericAttributeValue<PathReference> getControllerUrl();


  /**
   * Returns the value of the controllerClass child.
   * Attribute controllerClass
   *
   * @return the value of the controllerClass child.
   */
  @NotNull
  @ExtendClass("org.apache.struts.tiles.Controller")
  GenericAttributeValue<PsiClass> getControllerClass();


  /**
   * Returns the value of the page child.
   * Attribute page
   *
   * @return the value of the page child.
   */
  @NotNull
  GenericAttributeValue<PathReference> getPage();


  /**
   * Returns the value of the template child.
   * Attribute template
   *
   * @return the value of the template child.
   */
  @NotNull
  GenericAttributeValue<PathReference> getTemplate();


  /**
   * Returns the value of the path child.
   * Attribute path
   *
   * @return the value of the path child.
   */
  @NotNull
  GenericAttributeValue<PathReference> getPath();


  /**
   * Returns the value of the icon child.
   * Type icon documentation
   * <pre>
   *  The "icon" element contains a small-icon and large-icon element which
   *      specify the location, relative to the Struts configuration file, for small
   *      and large images used to represent the surrounding element in GUI tools.
   * </pre>
   *
   * @return the value of the icon child.
   */
  Icon getIcon();


  /**
   * Returns the value of the display-name child.
   * Type display-name documentation
   * <pre>
   *  The "display-name" element contains a short (one line) description of
   *      the surrounding element, suitable for use in GUI tools.
   * </pre>
   *
   * @return the value of the display-name child.
   */
  GenericDomValue<String> getDisplayName();


  /**
   * Returns the value of the description child.
   * Type description documentation
   * <pre>
   *  The "description" element contains descriptive (paragraph length) text
   *      about the surrounding element, suitable for use in GUI tools.
   * </pre>
   *
   * @return the value of the description child.
   */
  GenericDomValue<String> getDescription();


  /**
   * Returns the list of put children.
   * Type put documentation
   * <pre>
   *  The "put" element describes an attribute of a definition. It allows to
   *      specify the tiles attribute name and its value. The tiles value can be
   *      specified as an xml attribute, or in the body of the <put> tag.
   *      content         Same as value. For compatibility with the template tag library.
   *      direct          Same as type="string". For compatibility with the template
   *                      tag library.
   *      name            The unique identifier for this put.
   *      type            The type of the value. Can be: string, page, template or definition.
   *                      By default, no type is associated to a value. If a type is
   *                      associated, it will be used as a hint to process the value
   *                      when the attribute will be used in the inserted tiles.
   *      value           The value associated to this tiles attribute. The value should
   *                      be specified with this tag attribute, or in the body of the tag.
   * </pre>
   *
   * @return the list of put children.
   */
  @Stubbed
  List<Put> getPuts();

  @Stubbed
  List<Put> getPutAttributes();

  @SubTagsList({"put", "put-attribute"})
  @Stubbed
  List<Put> getAllPuts();


  /**
   * Adds new child to the list of put children.
   *
   * @return created child
   */
  Put addPut();


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
  @SubTagList("putList")
  List<PutList> getPutLists();

  /**
   * Adds new child to the list of putList children.
   *
   * @return created child
   */
  PutList addPutList();


  // 2.x/3.x ============

  List<PutListAttribute> getPutListAttributes();

  PutListAttribute addPutListAttribute();

  GenericAttributeValue<String> getTemplateType();

  GenericAttributeValue<String> getTemplateExpression();

  @ExtendClass(TilesConstants.VIEW_PREPARER)
  GenericAttributeValue<PsiClass> getPreparer();
}
