// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.grails.spring;

import com.intellij.facet.ModifiableFacetModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.spring.facet.SpringFacet;
import com.intellij.spring.facet.SpringFileSet;
import com.intellij.util.Consumer;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.plugins.grails.util.GrailsFacetProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public final class GrailsSpringFacetProvider implements GrailsFacetProvider {
  private static final @NonNls String GRAILS_FILESET = "Grails";

  private static final String[] configurationLocations = {"web-app/WEB-INF/applicationContext.xml", "grails-app/conf/spring/resources.xml"};

  @Override
  public void addFacets(Collection<Consumer<ModifiableFacetModel>> actions,
                        final Module module,
                        Collection<VirtualFile> roots) {
    final List<VirtualFile> configFiles = new ArrayList<>();

    for (VirtualFile root : roots) {
      for (String configurationLocation : configurationLocations) {
        final VirtualFile appContext = root.findFileByRelativePath(configurationLocation);
        if (appContext != null) {
          configFiles.add(appContext);
        }
      }
    }
    actions.add(model -> {
      Collection<SpringFacet> facets = model.getFacetsByType(SpringFacet.FACET_TYPE_ID);

      if (facets.isEmpty()) {
        var facetType = SpringFacet.getSpringFacetType();
        SpringFacet facet = facetType.createFacet(
          module, facetType.getPresentableName(), facetType.createDefaultConfiguration(), null
        );

        if (!configFiles.isEmpty()) {
          SpringFileSet fileSet = facet.addFileSet(GRAILS_FILESET, GRAILS_FILESET);

          for (VirtualFile configFile : configFiles) {
            fileSet.addFile(configFile);
          }
        }

        model.addFacet(facet);
      }
      else if (!configFiles.isEmpty()) {
        SpringFileSet fileSet = null;

        for (SpringFacet springFacet : facets) {
          for (SpringFileSet set : springFacet.getFileSets()) {
            if (GRAILS_FILESET.equals(set.getId())) {
              fileSet = set;
            }

            for (Iterator<VirtualFile> itr = configFiles.iterator(); itr.hasNext(); ) {
              VirtualFile file = itr.next();
              if (set.hasFile(file)) itr.remove();
            }
          }
        }

        if (fileSet == null) {
          SpringFacet facet = ContainerUtil.getFirstItem(facets);
          assert facet != null;
          fileSet = facet.addFileSet(GRAILS_FILESET, GRAILS_FILESET);
        }

        if (fileSet != null) {
          for (VirtualFile configFile : configFiles) {
            fileSet.addFile(configFile);
          }
        }
      }
    });
  }
}
