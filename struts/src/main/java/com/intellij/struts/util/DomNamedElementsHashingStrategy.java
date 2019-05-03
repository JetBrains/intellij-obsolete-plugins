/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
