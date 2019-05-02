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
