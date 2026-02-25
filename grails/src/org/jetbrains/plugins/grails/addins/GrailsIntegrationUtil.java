// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.addins;

import org.jetbrains.annotations.NonNls;

public final class GrailsIntegrationUtil {
  private static final boolean myJsSupportEnabled = classExists("com.intellij.lang.javascript.psi.JSElement");
  private static final boolean myCssSupportEnabled = classExists("com.intellij.psi.css.CssElement");

  private GrailsIntegrationUtil() {
  }

  private static boolean classExists(@NonNls String qname) {
    try {
      Class.forName(qname);
      return true;
    }
    catch (ClassNotFoundException e) {
      return false;
    }
  }

  public static boolean isJsSupportEnabled() {
    return myJsSupportEnabled;
  }

  public static boolean isCssSupportEnabled() {
    return myCssSupportEnabled;
  }

}
