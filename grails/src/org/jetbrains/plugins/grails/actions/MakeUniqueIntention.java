// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.actions;

import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.InheritanceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.references.domain.GormUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;

public final class MakeUniqueIntention extends DomainFieldIntention {

  public MakeUniqueIntention() {
    super("unique", true);
  }

  @Override
  protected boolean isAppropriateField(@NotNull GrField field, @NotNull PsiType fieldType) {
    if (InheritanceUtil.isInheritor(fieldType, CommonClassNames.JAVA_UTIL_COLLECTION)) return false;
    if (!GormUtils.isGormBean(field.getContainingClass())) return false;
    return true;
  }

  @Override
  public @NotNull String getText() {
    return GrailsBundle.message("intention.text.make.property.unique");
  }

}
