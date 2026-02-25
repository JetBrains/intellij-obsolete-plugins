// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.directive.GspDirective;

import java.util.HashMap;
import java.util.Map;

public enum GspDirectiveKind {
  PAGE("page", "tag"),
  INCLUDE("include"),
  TAGLIB("taglib"),
  ATTRIBUTE("attribute"),
  VARIABLE("variable");

  private final String[] tagNames;

  GspDirectiveKind(String ... tagNames) {
    this.tagNames = tagNames;
  }

  public String[] getTagNames() {
    return tagNames;
  }

  public boolean isInstance(@NotNull GspDirective directive) {
    return getKind(directive) == this;
  }

  public static @Nullable GspDirectiveKind getKind(@NotNull GspDirective directive) {
    return GspDirectiveKindStatic.KIND_MAP.get(directive.getName());
  }
}

final class GspDirectiveKindStatic {
  public static final Map<String, GspDirectiveKind> KIND_MAP = new HashMap<>();
  static {
    for (GspDirectiveKind kind : GspDirectiveKind.values()) {
      for (String tagName : kind.getTagNames()) {
        Object o = KIND_MAP.put(tagName, kind);
        assert o == null;
      }
    }
  }

  private GspDirectiveKindStatic() {
  }
}