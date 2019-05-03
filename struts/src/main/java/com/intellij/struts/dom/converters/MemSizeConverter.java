/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.dom.converters;

import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.Converter;
import org.jetbrains.annotations.Nullable;

/**
 * @author Dmitry Avdeev
 */
public class MemSizeConverter extends Converter<Long> {

  @Override
  public Long fromString(@Nullable final String s, final ConvertContext context) {
    if (s == null || s.length() == 0) {
      return null;
    }
    int multiplier;
    switch (s.charAt(s.length() - 1)) {
      case'K':
        multiplier = 1000;
        break;
      case'M':
        multiplier = 1000000;
        break;
      case'G':
        multiplier = 1000000000;
        break;
      default:
        multiplier = 1;
    }
    try {
      if (multiplier == 1) {
        return Long.decode(s);
      }
      else {
        return Long.decode(s.substring(0, s.length() - 1)).longValue() * multiplier;
      }
    }
    catch (NumberFormatException e) {
      return null;
    }
  }

  @Override
  public String toString(final Long aLong, final ConvertContext context) {
    return aLong == null ? null : aLong.toString();
  }
}
