// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain.persistent;

import com.intellij.jpa.model.common.persistence.mapping.AttributeOverride;
import com.intellij.jpa.model.common.persistence.mapping.Embedded;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.xml.GenericValue;
import com.intellij.util.xml.ReadOnlyGenericValue;

import java.util.Collections;
import java.util.List;

public class GormEmbeddedAttribute extends GormPersistentAttribute implements Embedded {
  public GormEmbeddedAttribute(GormEntity entity, String name, PsiType type, PsiElement element) {
    super(entity, name, type, element);
  }

  @Override
  public GenericValue<PsiClass> getTargetEmbeddableClass() {
    return ReadOnlyGenericValue.getInstance(PsiTypesUtil.getPsiClass(myType));
  }

  @Override
  public List<? extends AttributeOverride> getAttributeOverrides() {
    return Collections.emptyList();
  }
}
