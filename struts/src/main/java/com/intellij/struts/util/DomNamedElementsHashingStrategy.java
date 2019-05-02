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

package com.intellij.struts.util;

import com.intellij.util.xml.DomElement;
import gnu.trove.TObjectHashingStrategy;

/**
 * @author Dmitry Avdeev
 */
public class DomNamedElementsHashingStrategy<T extends DomElement> implements TObjectHashingStrategy<T> {

  @Override
  public int computeHashCode(final DomElement object) {
    return object.getGenericInfo().getElementName(object).hashCode();
  }

  @Override
  public boolean equals(final DomElement o1, final DomElement o2) {
    return o1.getGenericInfo().getElementName(o1).equals(o2.getGenericInfo().getElementName(o2));
  }
}
