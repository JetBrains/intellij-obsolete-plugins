// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.intellij.groovy.grails.hibernate;

import com.intellij.hibernate.model.HibernateConstants;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.persistence.model.PersistenceListener;
import com.intellij.persistence.model.PersistenceMappings;
import com.intellij.persistence.model.PersistencePackage;
import com.intellij.persistence.model.helpers.PersistenceUnitModelHelper;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.GenericValue;
import com.intellij.util.xml.ReadOnlyGenericValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.references.domain.persistent.GormPersistenceMapping;
import org.jetbrains.plugins.groovy.mvc.MvcModuleStructureSynchronizer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class GormSessionFactory extends UserDataHolderBase implements PersistencePackage, PersistenceUnitModelHelper {

  private static final GenericValue<String> NAME = ReadOnlyGenericValue.getInstance("Gorm");

  private final Module myModule;

  private final GormPersistenceMapping myPersistenceMapping;

  public GormSessionFactory(Module module) {
    myModule = module;
    myPersistenceMapping = new GormPersistenceMapping(module);
  }

  @Override
  public GenericValue<String> getName() {
    return NAME;
  }

  @Override
  public PersistenceUnitModelHelper getModelHelper() {
    return this;
  }

  @Override
  public boolean isValid() {
    return !myModule.isDisposed();
  }

  @Override
  public @Nullable XmlTag getXmlTag() {
    return null;
  }

  @Override
  public PsiManager getPsiManager() {
    return PsiManager.getInstance(myModule.getProject());
  }

  @Override
  public Module getModule() {
    return myModule;
  }

  @Override
  public PsiElement getIdentifyingPsiElement() {
    return null;
  }

  @Override
  public PsiFile getContainingFile() {
    return null;
  }

  @Override
  public GenericValue<Boolean> getExcludeUnlistedClasses() {
    return ReadOnlyGenericValue.getInstance(Boolean.TRUE);
  }

  @Override
  public @Nullable String getPersistenceProviderName() {
    return HibernateConstants.PERSISTENCE_PROVIDER_CLASS;
  }

  @Override
  public @Nullable PersistenceMappings getAdditionalMapping() {
    return myPersistenceMapping;
  }

  @Override
  public @NotNull <V extends PersistenceMappings> List<? extends GenericValue<V>> getMappingFiles(Class<V> mappingsClass) {
    return Collections.emptyList();
  }

  @Override
  public @NotNull List<? extends PersistenceListener> getPersistentListeners() {
    return Collections.emptyList();
  }

  @Override
  public @NotNull List<? extends GenericValue<PsiFile>> getJarFiles() {
    return Collections.emptyList();
  }

  @Override
  public @NotNull List<? extends GenericValue<PsiClass>> getClasses() {
    return Collections.emptyList();
    //Collection<? extends GrClassDefinition> domains = GrailsArtifact.DOMAIN.getInstances(myModule).values();
    //
    //List<GenericValue<PsiClass>> res = new ArrayList<GenericValue<PsiClass>>(domains.size());
    //
    //for (GrClassDefinition definition : domains) {
    //  res.add(ReadOnlyGenericValue.<PsiClass>getInstance(definition));
    //}
    //
    //return res;
  }

  @Override
  public @NotNull List<? extends GenericValue<PsiPackage>> getPackages() {
    return Collections.emptyList();

    //Set<String> packageNames = new HashSet<String>();
    //for (GrClassDefinition definition : GrailsArtifact.DOMAIN.getInstances(myModule).values()) {
    //  String qualifiedName = definition.getQualifiedName();
    //  if (qualifiedName != null) {
    //    packageNames.add(StringUtil.getPackageName(qualifiedName));
    //  }
    //}
    //
    //List<GenericValue<PsiPackage>> res = new ArrayList<GenericValue<PsiPackage>>(packageNames.size());
    //
    //JavaPsiFacade facade = JavaPsiFacade.getInstance(myModule.getProject());
    //
    //for (String packageName : packageNames) {
    //  PsiPackage aPackage = facade.findPackage(packageName);
    //  if (aPackage != null) {
    //    res.add(ReadOnlyGenericValue.getInstance(aPackage));
    //  }
    //}
    //
    //return res;
  }

  @Override
  public GenericValue<String> getDataSourceName() {
    return null;
  }

  @Override
  public @NotNull Properties getPersistenceUnitProperties() {
    return new Properties();
  }

  @Override
  public Collection<Object> getCacheDependencies() {
    MvcModuleStructureSynchronizer synchronizer = MvcModuleStructureSynchronizer.getInstance(myModule.getProject());
    return Collections.singletonList(synchronizer.getFileAndRootsModificationTracker());
  }
}
