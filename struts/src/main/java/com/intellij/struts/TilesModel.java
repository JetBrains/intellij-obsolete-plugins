/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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