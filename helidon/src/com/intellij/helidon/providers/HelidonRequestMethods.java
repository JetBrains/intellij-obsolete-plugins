// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.providers;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
public enum HelidonRequestMethods {
  REGISTER("register"),
  GET("get"),
  HEAD("head"),
  POST("post"),
  PUT("put"),
  PATCH("patch"),
  DELETE("delete"),
  OPTIONS("options"),
  TRACE("trace"),
  ANY_OF("anyOf"),
  UNKNOWN("???");

  private static final Function<HelidonRequestMethods, String> NAMING_FUNCTION = Enum::name;
  private final String myMethodName;

  HelidonRequestMethods(String methodName) {
    myMethodName = methodName;
  }

  public static String getDisplay(HelidonRequestMethods... methods) {
    return "[" + StringUtil.join(methods, NAMING_FUNCTION, "|") + "]";
  }

  public static HelidonRequestMethods getTypeByMethodName(String method) {
    for (HelidonRequestMethods value : HelidonRequestMethods.values()) {
      if (value.myMethodName.equals(method)) return value;
    }
    return UNKNOWN;
  }
}
