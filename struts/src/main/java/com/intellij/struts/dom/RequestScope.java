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
