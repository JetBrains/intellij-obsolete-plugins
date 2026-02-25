// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain.persistent;

import com.intellij.jpa.model.common.persistence.mapping.ColumnBase;
import com.intellij.jpa.model.common.persistence.mapping.Id;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;

public class GormIdAttribute extends GormPersistentAttribute implements Id {
  public GormIdAttribute(GormEntity entity, String name, PsiType type, PsiElement element) {
    super(entity, name, type, element);
  }

  @Override
  public boolean isIdAttribute() {
    return true;
  }

  @Override
  public ColumnBase getColumn() {
    return null;
  }

}
