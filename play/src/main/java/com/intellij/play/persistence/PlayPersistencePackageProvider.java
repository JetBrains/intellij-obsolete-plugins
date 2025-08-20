package com.intellij.play.persistence;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolderEx;
import com.intellij.persistence.extensions.PersistencePackagesProvider;
import com.intellij.persistence.facet.PersistenceFacet;
import com.intellij.persistence.model.PersistencePackage;
import com.intellij.play.model.PlaySessionFactory;
import com.intellij.play.utils.PlayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class PlayPersistencePackageProvider implements PersistencePackagesProvider {

  private static final Key<List<PersistencePackage>> PLAY_PERSISTENCE_PACKAGE_KEY = Key.create("PlayPersistencePackageProvider");

  @NotNull
  @Override
  public List<PersistencePackage> getPersistencePackages(PersistenceFacet facet) {
    Module module = facet.getModule();
    if (PlayUtils.isPlayInstalled(module.getProject())) {
      List<PersistencePackage> res = module.getUserData(PLAY_PERSISTENCE_PACKAGE_KEY);
      if (res == null) {
        res = Collections.singletonList(new PlaySessionFactory(module));
        res = ((UserDataHolderEx)module).putUserDataIfAbsent(PLAY_PERSISTENCE_PACKAGE_KEY, res);
      }
      return res;
    }
    return Collections.emptyList();
  }
}
