// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain.persistent;

import com.intellij.jpa.model.common.persistence.mapping.ElementCollection;
import com.intellij.jpa.model.xml.persistence.mapping.Enumerated;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.xml.GenericValue;
import com.intellij.util.xml.ReadOnlyGenericValue;

public class GormCollectionAttribute extends GormPersistentAttribute implements ElementCollection {
  public GormCollectionAttribute(GormEntity entity, String name, PsiType type, PsiElement element) {
    super(entity, name, type, element);
  }

  @Override
  public GenericValue<Enumerated> getEnumerated() {
    return ReadOnlyGenericValue.nullInstance();
  }

  @Override
  public GenericValue<PsiType> getComponentType() {
    return ReadOnlyGenericValue.getInstance(PsiUtil.extractIterableTypeParameter(myType, true));
  }

  @Override
  public boolean isContainer() {
    return true;
  }
}
