// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.addins.js;

import com.intellij.lang.Language;
import com.intellij.lang.css.CSSLanguage;

public final class CssIntegrationUtil {

  private CssIntegrationUtil() {
  }

  public static boolean isCssLanguage(Language childLanguage) {
    return childLanguage instanceof CSSLanguage;
  }
}