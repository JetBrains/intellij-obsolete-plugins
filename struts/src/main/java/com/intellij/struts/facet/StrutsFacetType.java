/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetConfiguration;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import com.intellij.facet.ui.DefaultFacetSettingsEditor;
import com.intellij.facet.ui.FacetEditor;
import com.intellij.facet.ui.MultipleFacetSettingsEditor;
import com.intellij.framework.detection.FacetBasedFrameworkDetector;
import com.intellij.framework.detection.FileContentPattern;
import com.intellij.j2ee.web.WebUtilImpl;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.ElementPattern;
import com.intellij.struts.facet.ui.MultipleStrutsFacetEditor;
import com.intellij.struts.facet.ui.StrutsFacetDefaultSettingsEditor;
import com.intellij.util.indexing.FileContent;
import icons.StrutsApiIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Set;

/**
 * @author nik
 */
public class StrutsFacetType extends FacetType<StrutsFacet, StrutsFacetConfiguration> {
  public static final FacetTypeId<StrutsFacet> ID = new FacetTypeId<>("struts");

  StrutsFacetType() {
    super(ID, "struts", "Struts", WebFacet.ID);
  }

  public static StrutsFacetType getInstance() {
    return findInstance(StrutsFacetType.class);
  }

  @Override
  public StrutsFacetConfiguration createDefaultConfiguration() {
    return new StrutsFacetConfiguration();
  }

  @Override
  public StrutsFacet createFacet(@NotNull final Module module, final String name, @NotNull final StrutsFacetConfiguration configuration, @Nullable final Facet underlyingFacet) {
    return new StrutsFacet(this, module, name, configuration, underlyingFacet);
  }

  @Override
  public Icon getIcon() {
    return StrutsApiIcons.ActionMapping;
  }

  @Override
  public String getHelpTopic() {
    return "reference.settings.project.structure.facets.struts.facet";
  }

  @Override
  public boolean isSuitableModuleType(ModuleType moduleType) {
    return moduleType instanceof JavaModuleType;
  }

  @Override
  public DefaultFacetSettingsEditor createDefaultConfigurationEditor(@NotNull final Project project, @NotNull final StrutsFacetConfiguration configuration) {
    return new StrutsFacetDefaultSettingsEditor(configuration.getValidationConfiguration());
  }

  @Override
  public MultipleFacetSettingsEditor createMultipleConfigurationsEditor(@NotNull final Project project, @NotNull final FacetEditor[] editors) {
    return new MultipleStrutsFacetEditor(editors);
  }

  public static class StrutsFrameworkDetector extends FacetBasedFrameworkDetector<StrutsFacet, StrutsFacetConfiguration> {
    public StrutsFrameworkDetector() {
      super("struts");
    }

    @NotNull
    @Override
    public FacetType<StrutsFacet, StrutsFacetConfiguration> getFacetType() {
      return StrutsFacetType.getInstance();
    }

    @NotNull
    @Override
    public FileType getFileType() {
      return StdFileTypes.XML;
    }

    @NotNull
    @Override
    public ElementPattern<FileContent> createSuitableFilePattern() {
      return FileContentPattern.fileContent().withName(AddStrutsSupportUtil.STRUTS_CONFIG_FILE_NAME).xmlWithRootTag("struts-config");
    }

    @Override
    public boolean isSuitableUnderlyingFacetConfiguration(FacetConfiguration underlying,
                                                          StrutsFacetConfiguration configuration,
                                                          Set<VirtualFile> files) {
      return WebUtilImpl.isWebFacetConfigurationContainingFiles(underlying, files);
    }
  }
}
