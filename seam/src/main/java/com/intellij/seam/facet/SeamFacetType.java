package com.intellij.seam.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.framework.detection.FacetBasedFrameworkDetector;
import com.intellij.framework.detection.FileContentPattern;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.patterns.ElementPattern;
import com.intellij.seam.SeamIcons;
import com.intellij.seam.constants.SeamConstants;
import com.intellij.seam.resources.SeamBundle;
import com.intellij.util.indexing.FileContent;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class SeamFacetType extends FacetType<SeamFacet, SeamFacetConfiguration> {
  SeamFacetType() {
    super(SeamFacet.FACET_TYPE_ID, "Seam", SeamBundle.SEAM_FRAMEWORK);
  }

  public static SeamFacetType getInstance() {
    return findInstance(SeamFacetType.class);
  }

  @Override
  public SeamFacetConfiguration createDefaultConfiguration() {
    return new SeamFacetConfiguration();
  }

  @Override
  public SeamFacet createFacet(@NotNull final Module module, final String name, @NotNull final SeamFacetConfiguration configuration,
                               final Facet underlyingFacet) {
    return new SeamFacet(this, module, name, configuration, underlyingFacet);
  }

  @Override
  public boolean isSuitableModuleType(final ModuleType moduleType) {
    return moduleType instanceof JavaModuleType;
  }

  @Override
  public Icon getIcon() {
    return SeamIcons.Seam;
  }

  public static class SeamFrameworkDetector extends FacetBasedFrameworkDetector<SeamFacet, SeamFacetConfiguration> {
    public SeamFrameworkDetector() {
      super("seam");
    }

    @NotNull
    @Override
    public FacetType<SeamFacet, SeamFacetConfiguration> getFacetType() {
      return SeamFacetType.getInstance();
    }

    @NotNull
    @Override
    public FileType getFileType() {
      return XmlFileType.INSTANCE;
    }

    @NotNull
    @Override
    public ElementPattern<FileContent> createSuitableFilePattern() {
      return FileContentPattern.fileContent().withName(SeamConstants.SEAM_CONFIG_FILENAME).xmlWithRootTag(SeamConstants.SEAM_CONFIG_ROOT_TAG_NAME);
    }
  }
}
