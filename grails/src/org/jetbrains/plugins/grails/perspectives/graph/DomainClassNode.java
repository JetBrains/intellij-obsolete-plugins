// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.perspectives.graph;

import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class DomainClassNode {
  private final @NotNull PsiClass myTypeDefinition;

  public DomainClassNode(@NotNull PsiClass typeDefinition) {
    myTypeDefinition = typeDefinition;
  }

  public @NotNull String getUniqueName() {
    final String qualifiedName = myTypeDefinition.getQualifiedName();
    if (qualifiedName != null) {
      return qualifiedName;
    }

    return myTypeDefinition.getName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DomainClassNode that = (DomainClassNode) o;
    return Objects.equals(myTypeDefinition, that.myTypeDefinition);
  }

  @Override
  public int hashCode() {
    return myTypeDefinition.hashCode();
  }

  public @NotNull PsiClass getTypeDefinition() {
    return myTypeDefinition;
  }
}
