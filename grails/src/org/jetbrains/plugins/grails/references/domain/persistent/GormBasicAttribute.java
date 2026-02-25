// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain.persistent;

import com.intellij.jpa.model.common.persistence.mapping.Basic;
import com.intellij.jpa.model.common.persistence.mapping.ColumnBase;
import com.intellij.jpa.model.xml.persistence.mapping.Enumerated;
import com.intellij.jpa.model.xml.persistence.mapping.FetchType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.util.xml.GenericValue;
import com.intellij.util.xml.ReadOnlyGenericValue;

public class GormBasicAttribute extends GormPersistentAttribute implements Basic {
  public GormBasicAttribute(GormEntity entity, String name, PsiType type, PsiElement element) {
    super(entity, name, type, element);
  }

  @Override
  public GenericValue<FetchType> getFetch() {
    return ReadOnlyGenericValue.nullInstance();
  }

  @Override
  public GenericValue<String> getLob() {
    return ReadOnlyGenericValue.nullInstance();
  }

  @Override
  public GenericValue<Boolean> getOptional() {
    return ReadOnlyGenericValue.getInstance(false);
  }

  @Override
  public GenericValue<Enumerated> getEnumerated() {
    return ReadOnlyGenericValue.nullInstance();
  }

  @Override
  public ColumnBase getColumn() {
    return null;
  }
}
