// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain.persistent;

import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.openapi.module.Module;
import com.intellij.persistence.model.PersistenceListener;
import com.intellij.persistence.model.PersistenceMappings;
import com.intellij.persistence.model.PersistenceQuery;
import com.intellij.persistence.model.PersistentEmbeddable;
import com.intellij.persistence.model.PersistentEntity;
import com.intellij.persistence.model.PersistentSuperclass;
import com.intellij.persistence.model.helpers.PersistenceMappingsModelHelper;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.util.PropertyMemberType;
import com.intellij.util.xml.GenericValue;
import com.intellij.util.xml.ReadOnlyGenericValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GormPersistenceMapping extends CommonModelElement.ModuleBase implements PersistenceMappings, PersistenceMappingsModelHelper {

  private final Module myModule;

  public GormPersistenceMapping(Module module) {
    myModule = module;
  }

  @Override
  public @NotNull Module getModule() {
    return myModule;
  }

  @Override
  public PersistenceMappingsModelHelper getModelHelper() {
    return this;
  }

  @Override
  public GenericValue<PsiPackage> getPackage() {
    return ReadOnlyGenericValue.nullInstance();
  }

  @Override
  public PropertyMemberType getDeclaredAccess() {
    return null;
  }

  @Override
  public List<? extends PersistenceListener> getPersistentListeners() {
    return Collections.emptyList();
  }

  @Override
  public @NotNull List<? extends PersistentEntity> getPersistentEntities() {
    List<PersistentEntity> res = new ArrayList<>();

    for (GrClassDefinition domainClass : GrailsArtifact.DOMAIN.getInstances(myModule).values()) {
      PersistentEntity e = new GormEntity(myModule, domainClass);
      res.add(e);
    }

    return res;
  }

  @Override
  public @NotNull List<? extends PersistentSuperclass> getPersistentSuperclasses() {
    return Collections.emptyList();
  }

  @Override
  public @NotNull List<? extends PersistentEmbeddable> getPersistentEmbeddables() {
    return Collections.emptyList();
  }

  @Override
  public List<? extends PersistenceQuery> getNamedQueries() {
    return Collections.emptyList();
  }

  @Override
  public List<? extends PersistenceQuery> getNamedNativeQueries() {
    return Collections.emptyList();
  }
}
