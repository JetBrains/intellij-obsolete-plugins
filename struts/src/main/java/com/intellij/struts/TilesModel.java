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

package com.intellij.struts;

import com.intellij.psi.xml.XmlTag;
import com.intellij.struts.dom.tiles.Definition;
import com.intellij.struts.dom.tiles.Put;
import com.intellij.struts.dom.tiles.TilesDefinitions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface TilesModel extends NamedDomModel<TilesDefinitions> {

  /**
   * Returns the XmlTag for the Definition with the given name.
   *
   * @param definitionName Name of the Definition.
   * @return null if Definition not found.
   */
  @Nullable
  XmlTag getTileTag(String definitionName);

  /**
   * Returns a list of all Definitions.
   *
   * @return All Definitions.
   */
  @NotNull
  List<Definition> getDefinitions();

  /**
   * Returns the Definition with the given name.
   *
   * @param definitionName Name of the Definition.
   * @return null if Definition not found.
   */
  @Nullable
  Definition findDefinition(String definitionName);

  /**
   * Returns the Puts within the Definition.
   *
   * @param definitionName   Name of the Definition.
   * @param includingExtends Whether to include the Puts from extended Definition(s).
   * @return null if Definition not found.
   */
  @Nullable
  Set<Put> getPuts(String definitionName, boolean includingExtends);

  /**
   * Returns all Puts for the Definition.
   *
   * @param definitionName Name of the Definition.
   * @return All Puts.
   */
  @Nullable
  Collection<Put> getAllPuts(String definitionName);

  /**
   * Returns the XmlTag for the Put within the Definition using the given names.
   *
   * @param definitionName Name of the Definition.
   * @param putName        Name of the Put.
   * @return null if Put not found.
   */
  @Nullable
  XmlTag getPutTag(String definitionName, String putName);

}