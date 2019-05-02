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
