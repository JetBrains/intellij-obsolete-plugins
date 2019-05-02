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
import com.intellij.struts.StrutsConstants;
import com.intellij.struts.dom.StrutsRootElement;
import com.intellij.util.xml.Namespace;
import com.intellij.util.xml.Stubbed;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * tiles-config_1_3.dtd:tiles-definitions interface.
 * Type tiles-definitions documentation
 * <pre>
 *  The "tiles-definitions" element is the root of the configuration file
 *      hierarchy, and contains nested elements for all of the other
 *      configuration settings.
 * </pre>
 */
@Namespace(StrutsConstants.TILES_DOM_NAMESPACE_KEY)
@Presentation(icon = "StrutsApiIcons.Tiles.TilesConfig")
@Stubbed
public interface TilesDefinitions extends StrutsRootElement {

  @NonNls String TILES_DEFINITIONS = "tiles-definitions";

  /**
   * Returns the list of definition children.
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
   *
   * @return the list of definition children.
   */
  @NotNull
  @Stubbed
  List<Definition> getDefinitions();

  /**
   * Adds new child to the list of definition children.
   *
   * @return created child
   */
  Definition addDefinition();


}
