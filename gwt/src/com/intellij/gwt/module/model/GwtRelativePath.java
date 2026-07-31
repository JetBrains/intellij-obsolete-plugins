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

package com.intellij.gwt.module.model;

import com.intellij.util.xml.Attribute;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface GwtRelativePath extends DomElement {

  GenericAttributeValue<String> getPath();

  @Attribute("casesensitive")
  GenericAttributeValue<Boolean> isCaseSensitive();

  @Attribute("defaultexcludes")
  GenericAttributeValue<String> getDefaultExcludes();

  default @Nullable Boolean isDefaultExcludes() {
    String value = getDefaultExcludes().getValue();
    if (value == null) return null;
    if ("yes".equals(value)) return Boolean.TRUE;
    if ("no".equals(value)) return Boolean.FALSE;
    return null;
  }

  @Attribute("includes")
  GenericAttributeValue<String> getIncludesAttribute();

  @Attribute("excludes")
  GenericAttributeValue<String> getExcludesAttribute();

  List<GwtFileSet> getIncludes();

  List<GwtFileSet> getExcludes();
}
