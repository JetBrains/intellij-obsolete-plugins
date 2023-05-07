package com.intellij.play.model;

import com.intellij.ide.presentation.Presentation;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.persistence.model.PersistenceListener;
import com.intellij.persistence.model.PersistenceMappings;
import com.intellij.persistence.model.PersistencePackage;
import com.intellij.persistence.model.helpers.PersistenceUnitModelHelper;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.GenericValue;
import com.intellij.util.xml.ReadOnlyGenericValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@Presentation(icon = "com.intellij.play.PlayIcons.Play")
public class PlaySessionFactory extends UserDataHolderBase implements PersistencePackage, PersistenceUnitModelHelper {

  private static final GenericValue<String> NAME = ReadOnlyGenericValue.getInstance("Play!");

  private final Module myModule;

  public PlaySessionFactory(Module module) {
    myModule = module;
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

  @Nullable
  @Override
  public XmlTag getXmlTag() {
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
    return ReadOnlyGenericValue.getInstance(Boolean.FALSE);
  }

  @Nullable
  @Override
  public String getPersistenceProviderName() {
    return "org.hibernate.ejb.HibernatePersistence";
  }

  @Override
  @Nullable
  public PersistenceMappings getAdditionalMapping() {
    return null;
  }

  @NotNull
  @Override
  public <V extends PersistenceMappings> List<? extends GenericValue<V>> getMappingFiles(Class<V> mappingsClass) {
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public List<? extends PersistenceListener> getPersistentListeners() {
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public List<? extends GenericValue<PsiFile>> getJarFiles() {
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public List<? extends GenericValue<PsiClass>> getClasses() {
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public List<? extends GenericValue<PsiPackage>> getPackages() {
    PsiPackage models = JavaPsiFacade.getInstance(myModule.getProject()).findPackage("models");

    return models == null ? Collections.emptyList() : Collections.singletonList(ReadOnlyGenericValue.getInstance(models));
  }

  @Override
  public GenericValue<String> getDataSourceName() {
    return null;
  }

  @NotNull
  @Override
  public Properties getPersistenceUnitProperties() {
    return new Properties();
  }

  @Override
  public Collection<Object> getCacheDependencies() {
    return Collections.emptyList();
  }
}
