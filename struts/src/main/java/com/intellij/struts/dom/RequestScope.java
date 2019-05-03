/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

// Generated on Wed Apr 05 15:23:26 MSD 2006
// DTD/Schema  :    struts-config_1_3.dtd

package com.intellij.struts.dom;

import com.intellij.util.xml.NamedEnum;
import org.jetbrains.annotations.NonNls;

/**
 * struts-config_1_3.dtd:RequestScope enumeration.
 * Type RequestScope documentation
 * <pre>
 *  The name of a JSP bean scope within which such a form bean may be
 *      accessed.
 * </pre>
 */
public enum RequestScope implements NamedEnum {

  @NonNls request("request"),
  @NonNls session("session");

  private final String value;

  RequestScope(String value) {
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }

}
