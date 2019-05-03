/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
