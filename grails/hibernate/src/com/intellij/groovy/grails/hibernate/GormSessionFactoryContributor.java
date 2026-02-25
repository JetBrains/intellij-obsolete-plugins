// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.intellij.groovy.grails.hibernate;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Key;
import com.intellij.persistence.extensions.PersistencePackagesProvider;
import com.intellij.persistence.facet.PersistenceFacet;
import com.intellij.persistence.model.PersistencePackage;
import com.intellij.util.ConcurrencyUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.config.GrailsStructure;

import java.util.Collections;
import java.util.List;

final class GormSessionFactoryContributor implements PersistencePackagesProvider {
  private static final Key<List<PersistencePackage>> SESSION_FACTORY_KEY = Key.create("GormSessionFactoryContributor.SESSION_FACTORY_KEY");

  @Override
  public @NotNull List<PersistencePackage> getPersistencePackages(PersistenceFacet facet) {
    Module module = facet.getModule();
    GrailsStructure instance = GrailsStructure.getInstance(module);
    if (instance == null) return Collections.emptyList();

    return ConcurrencyUtil.computeIfAbsent(module, SESSION_FACTORY_KEY, () -> Collections.singletonList(new GormSessionFactory(module)));
  }
}
