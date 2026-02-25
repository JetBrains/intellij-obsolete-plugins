// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.intellij.groovy.grails.hibernate;

import com.intellij.facet.ModifiableFacetModel;
import com.intellij.hibernate.facet.HibernateFacet;
import com.intellij.hibernate.facet.HibernateFacetType;
import com.intellij.hibernate.model.HibernateDescriptorsConstants;
import com.intellij.jpa.facet.JpaFacet;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import com.intellij.util.descriptors.ConfigFileInfo;
import com.intellij.util.descriptors.ConfigFileInfoSet;
import org.jetbrains.plugins.grails.util.GrailsFacetProvider;
import org.jetbrains.plugins.grails.util.GrailsUtils;

import java.util.Collection;

final class GormHibernateFacetProvider implements GrailsFacetProvider {

  @Override
  public void addFacets(Collection<Consumer<ModifiableFacetModel>> actions,
                        final Module module,
                        Collection<VirtualFile> roots) {
    actions.add(model -> {
      final Collection<HibernateFacet> hibernateFacets = model.getFacetsByType(HibernateFacet.ID);
      final Collection<JpaFacet> jpaFacets = model.getFacetsByType(JpaFacet.ID);
      if (!hibernateFacets.isEmpty() || !jpaFacets.isEmpty()) return;
      HibernateFacetType facetType = HibernateFacetType.getInstance();
      HibernateFacet facet = facetType.createFacet(module, facetType.getPresentableName(), facetType.createDefaultConfiguration(), null);

      VirtualFile confDirectory = GrailsUtils.findConfDirectory(module);
      if (confDirectory != null) {
        VirtualFile hibernateDir = confDirectory.findChild("hibernate");
        if (hibernateDir != null) {
          ConfigFileInfoSet cfg = facet.getConfiguration().getDescriptorsConfiguration();

          VirtualFile hibernateCfgXml = hibernateDir.findChild("hibernate.cfg.xml");
          if (hibernateCfgXml != null) {
            cfg.addConfigFile(
              new ConfigFileInfo(HibernateDescriptorsConstants.HIBERNATE_CONFIGURATION_META_DATA, hibernateCfgXml.getUrl()));
          }

          //VirtualFile[] children = hibernateDir.getChildren();
          //if (children != null) {
          //  for (VirtualFile child : children) {
          //    if (!child.isDirectory() && child.getName().endsWith(".hbm.xml")) {
          //
          //    }
          //  }
          //}
        }
      }

      model.addFacet(facet);
    });
  }
}
