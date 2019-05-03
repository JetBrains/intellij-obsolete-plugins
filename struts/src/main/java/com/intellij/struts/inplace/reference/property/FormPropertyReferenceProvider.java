/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.reference.property;

import com.intellij.openapi.util.TextRange;
import com.intellij.struts.dom.FormProperty;

/**
 * @author davdeev
 */
public class FormPropertyReferenceProvider extends PropertyReferenceProvider {

  public FormPropertyReferenceProvider() {
    super(FormProperty.class);
  }

  @Override
  protected PropertyReference createReference(PropertyReferenceSet set, int index, TextRange range) {
    return new FormPropertyReference(set, index, range, true, this);
  }
}
