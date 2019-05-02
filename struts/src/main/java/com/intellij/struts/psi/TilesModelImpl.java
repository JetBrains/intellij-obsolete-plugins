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

package com.intellij.struts.psi;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.struts.NamedModelImpl;
import com.intellij.struts.TilesModel;
import com.intellij.struts.dom.tiles.Definition;
import com.intellij.struts.dom.tiles.Put;
import com.intellij.struts.dom.tiles.TilesDefinitions;
import com.intellij.struts.util.DomNamedElementsHashingStrategy;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomUtil;
import gnu.trove.THashSet;
import gnu.trove.TObjectHashingStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;


/**
 * Provides functionality for accessing DOM-Model of {@code tiles-defs.xml} files.
 */
public class TilesModelImpl extends NamedModelImpl<TilesDefinitions> implements TilesModel {

  public TilesModelImpl(@NotNull Set<XmlFile> configFiles, @NotNull DomFileElement<TilesDefinitions> mergedModel, String name) {
    super(configFiles, mergedModel, name);
  }

  @Override
  @Nullable
  public XmlTag getTileTag(final String definitionName) {
    final Definition def = findDefinition(definitionName);
    return def == null ? null : def.getName().getXmlTag();
  }

  @Override
  @NotNull
  public List<Definition> getDefinitions() {
    return getMergedModel().getDefinitions();
  }

  @Override
  @Nullable
  public Definition findDefinition(final String definitionName) {
    final List<Definition> defs = getDefinitions();

    final Definition exactMatch = DomUtil.findByName(defs, definitionName);
    if (exactMatch != null) {
      return exactMatch;
    }

    for (Definition definition : defs) {
      final String name = definition.getName().getStringValue();
      if (name == null || !StringUtil.containsChar(name, '*')) continue;

      if (Pattern.matches(StringUtil.replace(name, "*", "[^/]*"), definitionName)) {
        return definition;
      }
    }
    return null;
  }

  @Override
  @Nullable
  public Set<Put> getPuts(final String definitionName, boolean includingExtends) {
    Set<Put> set = new THashSet<>(putHashingStrategy);
    boolean found = getPuts(findDefinition(definitionName), set, new HashSet<>(), includingExtends);
    if (!found) {
      return null;
    }
    return set;
  }

  @Override
  @Nullable
  public Collection<Put> getAllPuts(final String definitionName) {
    ArrayList<Put> set = new ArrayList<>();
    boolean found = getPuts(findDefinition(definitionName), set, new HashSet<>(), true);
    if (!found) {
      return null;
    }
    return set;
  }

  @Override
  @Nullable
  public XmlTag getPutTag(final String definitionName, final String putName) {
    Definition def = findDefinition(definitionName);
    if (def == null) {
      return null;
    }
    Put put = findPutDefinition(def, putName, new HashSet<>());
    return put == null ? null : put.getName().getXmlTag();
  }

  @Nullable
  private static Put findPutDefinition(final Definition def, final String putName, final Set<Definition> visited) {
    Definition extend = def.getExtends().getValue();
    if (extend != null && !visited.contains(def)) {
      visited.add(extend);
      final Put put = findPutDefinition(extend, putName, visited);
      if (put != null) {
        return put;
      }
    }
    return DomUtil.findByName(def.getPuts(), putName);
  }

  private final static TObjectHashingStrategy<Put> putHashingStrategy = new DomNamedElementsHashingStrategy<>();

  private static boolean getPuts(final Definition definition, final Collection<Put> puts, final Set<Definition> visited, boolean includingExtends) {

    if (definition == null || visited.contains(definition)) {
      return false;
    }
    visited.add(definition);

    final List<Put> result = definition.getPuts();
    if (result != null) {
      for (Put put : result) {
        if (put.getName().getValue() != null) {
          puts.add(put);
        }
      }
    }
    if (includingExtends) {
      final Definition extend = definition.getExtends().getValue();
      if (extend != null) {
        getPuts(extend, puts, visited, true);
      }
    }
    return true;
  }
}
