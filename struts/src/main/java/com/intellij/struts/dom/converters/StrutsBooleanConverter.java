/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.dom.converters;

import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.Converter;

/**
 * @author Dmitry Avdeev
 */
public class StrutsBooleanConverter extends Converter<Boolean> {

  @Override
  public Boolean fromString(final String s, final ConvertContext context) {
    if (s == null) {
      return null;
    }
    else if (s.equals("true") || s.equals("yes")) {
      return Boolean.TRUE;
    }
    else if (s.equals("false") || s.equals("no")) {
      return Boolean.FALSE;
    }
    else {
      return null;
    }
  }

  @Override
  public String toString(final Boolean t, final ConvertContext context) {
    return t == null ? null : t.toString();
  }
}
