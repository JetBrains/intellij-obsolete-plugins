// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.actions;

import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;

public final class MakeNullableIntention extends DomainFieldIntention {

  public MakeNullableIntention() {
    super("nullable", true);
  }

  @Override
  protected boolean isAppropriateField(@NotNull GrField field, @NotNull PsiType fieldType) {
    return !(fieldType instanceof PsiPrimitiveType);
  }

  @Override
  public @NotNull String getText() {
    return GrailsBundle.message("intention.text.make.property.nullable");
  }

}
