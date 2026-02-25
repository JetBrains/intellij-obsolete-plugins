// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain.persistent;

import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.jpa.model.common.persistence.mapping.AttributeBase;
import com.intellij.jpa.model.common.persistence.mapping.PersistentObject;
import com.intellij.persistence.model.helpers.PersistentAttributeModelHelper;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiType;
import com.intellij.util.xml.GenericValue;
import com.intellij.util.xml.ReadOnlyGenericValue;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class GormPersistentAttribute extends CommonModelElement.PsiBase implements AttributeBase, PersistentAttributeModelHelper {

  protected final GormEntity myEntity;
  protected final GenericValue<String> myName;
  protected final PsiElement myElement;
  protected final PsiType myType;

  public GormPersistentAttribute(GormEntity entity, String name, PsiType type, PsiElement element) {
    myEntity = entity;
    myName = ReadOnlyGenericValue.getInstance(name);
    myElement = element;
    myType = type;
  }

  @Override
  public GenericValue<String> getName() {
    return myName;
  }

  @Override
  public PsiMember getPsiMember() {
    return myElement instanceof PsiMember ? (PsiMember)myElement : null;
  }

  @Override
  public PersistentObject getPersistentObject() {
    return myEntity;
  }

  @Override
  public PsiType getPsiType() {
    return myType;
  }

  @Override
  public PersistentAttributeModelHelper getAttributeModelHelper() {
    return this;
  }

  @Override
  public @NotNull PsiElement getPsiElement() {
    return myElement;
  }

  @Override
  public boolean isFieldAccess() {
    return false;
  }

  @Override
  public boolean isIdAttribute() {
    return false;
  }

  @Override
  public List<? extends GenericValue<String>> getMappedColumns() {
    return Collections.emptyList();
  }

  @Override
  public boolean isContainer() {
    return false;
  }

  @Override
  public boolean isLob() {
    return false;
  }

  @Override
  public GenericValue<PsiClass> getMapKeyClass() {
    return ReadOnlyGenericValue.nullInstance();
  }
}
