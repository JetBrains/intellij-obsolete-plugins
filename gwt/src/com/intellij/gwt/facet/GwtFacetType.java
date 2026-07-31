/*
 * Copyright 2000-2006 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.gwt.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import com.intellij.facet.ui.DefaultFacetSettingsEditor;
import com.intellij.facet.ui.FacetEditor;
import com.intellij.facet.ui.MultipleFacetSettingsEditor;
import com.intellij.framework.detection.FacetBasedFrameworkDetector;
import com.intellij.framework.detection.FileContentPattern;
import com.intellij.gwt.icons.GwtIcons;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.StandardPatterns;
import com.intellij.util.indexing.FileContent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.gwt.model.impl.GwtExternalizationConstants;

import javax.swing.Icon;

public final class GwtFacetType extends FacetType<GwtFacet, GwtFacetConfiguration> {
  public static final FacetTypeId<GwtFacet> ID = new FacetTypeId<>("gwt");

  GwtFacetType() {
    super(ID, GwtExternalizationConstants.GWT_FACET_ID, GwtExternalizationConstants.GWT_FACET_NAME);
  }

  public static GwtFacetType getInstance() {
    return FacetType.findInstance(GwtFacetType.class);
  }

  @Override
  public GwtFacetConfiguration createDefaultConfiguration() {
    return new GwtFacetConfiguration();
  }

  @Override
  public GwtFacet createFacet(@NotNull Module module, final String name, @NotNull GwtFacetConfiguration configuration, @Nullable Facet underlyingFacet) {
    return new GwtFacet(this, module, name, configuration);
  }

  @Override
  public boolean isSuitableModuleType(ModuleType moduleType) {
    return moduleType instanceof JavaModuleType;
  }

  @Override
  public String getHelpTopic() {
    return "IntelliJ.IDEA.Procedures.Java.EE.Development.Managing.Facets.Facet.Specific.Settings.GWT";
  }

  @Override
  public Icon getIcon() {
    return GwtIcons.GoogleSmall;
  }

  @Override
  public DefaultFacetSettingsEditor createDefaultConfigurationEditor(final @NotNull Project project, final @NotNull GwtFacetConfiguration configuration) {
    return new DefaultGwtFacetSettingsEditor(project, configuration);
  }

  @Override
  public MultipleFacetSettingsEditor createMultipleConfigurationsEditor(final @NotNull Project project, final FacetEditor @NotNull [] editors) {
    return new MultipleGwtFacetSettingsEditor(project, editors);
  }

  public static final class GwtFrameworkDetector extends FacetBasedFrameworkDetector<GwtFacet, GwtFacetConfiguration> {
    public GwtFrameworkDetector() {
      super("gwt-detector");
    }

    @Override
    public @NotNull FacetType<GwtFacet, GwtFacetConfiguration> getFacetType() {
      return getInstance();
    }

    @Override
    public @NotNull FileType getFileType() {
      return XmlFileType.INSTANCE;
    }

    @Override
    public @NotNull ElementPattern<FileContent> createSuitableFilePattern() {
      return FileContentPattern.fileContent().withName(StandardPatterns.string().endsWith(".gwt.xml"));
    }

    @Override
    public void setupFacet(@NotNull GwtFacet facet, ModifiableRootModel model) {
      final GwtFacetConfiguration configuration = facet.getConfiguration();
      GwtFacet.setupGwtSdkAndLibraries(configuration, model, configuration.getSdk());
    }
  }
}
