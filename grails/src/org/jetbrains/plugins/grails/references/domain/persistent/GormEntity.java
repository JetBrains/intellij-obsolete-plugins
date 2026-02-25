// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain.persistent;

import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.jpa.model.common.persistence.mapping.AttributeBase;
import com.intellij.jpa.model.common.persistence.mapping.PersistentObject;
import com.intellij.jpa.model.xml.persistence.mapping.AccessType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Pair;
import com.intellij.persistence.model.PersistenceInheritanceType;
import com.intellij.persistence.model.PersistenceQuery;
import com.intellij.persistence.model.PersistentAttribute;
import com.intellij.persistence.model.PersistentEntity;
import com.intellij.persistence.model.TableInfoProvider;
import com.intellij.persistence.model.helpers.PersistentEntityModelHelper;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PropertyMemberType;
import com.intellij.util.xml.GenericValue;
import com.intellij.util.xml.ReadOnlyGenericValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.domain.DomainDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GormEntity extends CommonModelElement.PsiBase implements PersistentEntity, PersistentEntityModelHelper, PersistentObject, TableInfoProvider {

  private final Module myModule;
  private final PsiClass myDomainClass;

  private volatile List<AttributeBase> myAttributes;
  
  public GormEntity(Module module, PsiClass domainClass) {
    myModule = module;
    myDomainClass = domainClass;
  }

  @Override
  public GenericValue<String> getName() {
    return ReadOnlyGenericValue.getInstance(myDomainClass.getName());
  }

  @Override
  public GenericValue<PsiClass> getClazz() {
    return ReadOnlyGenericValue.getInstance(myDomainClass);
  }

  @Override
  public List<? extends AttributeBase> getAllAttributes() {
    List<AttributeBase> res = myAttributes;
    if (res == null) {
      res = new ArrayList<>();

      DomainDescriptor descriptor = DomainDescriptor.getDescriptor(myDomainClass);
      
      for (Map.Entry<String, Pair<PsiType, PsiElement>> entry : descriptor.getPersistentProperties().entrySet()) {
        if (descriptor.isToManyRelation(entry.getKey())) {
          res.add(new GormCollectionAttribute(this, entry.getKey(), entry.getValue().first, entry.getValue().second));
        }
        else {
          GormPersistentAttribute attr;

          if ("id".equals(entry.getKey())) {
            attr = new GormIdAttribute(this, entry.getKey(), entry.getValue().first, entry.getValue().second);
          }
          else if (descriptor.getEmbeddedPropertyNames().contains(entry.getKey())) {
            attr = new GormEmbeddedAttribute(this, entry.getKey(), entry.getValue().first, entry.getValue().second);
          }
          else {
            attr = new GormBasicAttribute(this, entry.getKey(), entry.getValue().first, entry.getValue().second);
          }

          res.add(attr);
        }
      }

      myAttributes = res;
    }

    return res;
  }

  @Override
  public AccessType getEffectiveAccessType() {
    return null;
  }

  @Override
  public @NotNull PersistentEntityModelHelper getObjectModelHelper() {
    return this;
  }

  @Override
  public GenericValue<PsiClass> getIdClassValue() {
    return ReadOnlyGenericValue.nullInstance();
  }

  @Override
  public @NotNull PsiElement getPsiElement() {
    return myDomainClass;
  }

  @Override
  public Module getModule() {
    return myModule;
  }

  @Override
  public TableInfoProvider getTable() {
    return this;
  }

  @Override
  public GenericValue<String> getTableName() {
    return ReadOnlyGenericValue.nullInstance();
  }

  @Override
  public GenericValue<String> getCatalog() {
    return ReadOnlyGenericValue.nullInstance();
  }

  @Override
  public GenericValue<String> getSchema() {
    return ReadOnlyGenericValue.nullInstance();
  }

  @Override
  public List<? extends TableInfoProvider> getSecondaryTables() {
    return Collections.emptyList();
  }

  @Override
  public PersistenceInheritanceType getInheritanceType(PersistentEntity descendant) {
    return null;
  }

  @Override
  public List<? extends PersistenceQuery> getNamedQueries() {
    return Collections.emptyList();
  }

  @Override
  public List<? extends PersistenceQuery> getNamedNativeQueries() {
    return Collections.emptyList();
  }

  @Override
  public @NotNull List<? extends PersistentAttribute> getAttributes() {
    return getAllAttributes();
  }

  @Override
  public PropertyMemberType getDefaultAccessMode() {
    return null;
  }

  @Override
  public boolean isAccessModeFixed() {
    return false;
  }

}
