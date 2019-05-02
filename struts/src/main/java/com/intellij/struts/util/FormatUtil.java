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

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Various utilities for manipulating (URI-related) strings.
 *
 * @author Dmitry Avdeev
 */
public class FormatUtil {

  private FormatUtil() {
  }

  /**
   * http://jakarta.apache.org/commons/dtds/validator_1_1_3.dtd -> 1_1_3
   * http://jakarta.apache.org/commons/dtds/validator_1_0.dtd -> 1_0
   *
   * @param uri e.g. http://jakarta.apache.org/commons/dtds/validator_1_1_3.dtd
   * @return extracted version
   */
  @Nullable
  public static String getVersion(@NotNull @NonNls final String uri) {
    int pos = uri.indexOf('_');
    int dtdPos = uri.lastIndexOf('.');
    if (pos == -1 || dtdPos == -1 || dtdPos <= pos) {
      return null;
    }
    return uri.substring(pos + 1, dtdPos);
  }

  public static String replace(@NonNls final String source, char from, char to) {
    StringBuilder buf = new StringBuilder(source);
    for (int i = 0; i < buf.length(); i++) {
      if (buf.charAt(i) == from) {
        buf.setCharAt(i, to);
      }
    }
    return buf.toString();
  }
}
