package com.intellij.gwt.model;

import com.intellij.configurationStore.StoreUtil;
import com.intellij.facet.FacetManager;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.facet.GwtFacetConfiguration;
import com.intellij.gwt.references.GwtCodeInsightTestCase;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;

import static org.assertj.core.api.Assertions.assertThat;

public class GwtFacetConfigurationTest extends GwtCodeInsightTestCase {
  public void testRecursivePathToMacroExpansion() throws Exception {
    Module module = myCodeInsightFixture.getModule();
    GwtFacet gwtFacet = GwtFacet.getInstance(module);
    assertThat(gwtFacet).isNotNull();

    GwtFacetConfiguration configuration = gwtFacet.getConfiguration();
    String path = myCodeInsightFixture.findFileInTempDir("empty.txt").getPath();
    configuration.setAdditionalCompilerVMParameters("-additionalParam " + path);
    configuration.setCompilerParameters("-param " + path);
    FacetManager.getInstance(module).facetConfigurationChanged(gwtFacet);
    StoreUtil.saveSettings(myCodeInsightFixture.getProject());

    VirtualFile imlFile = myCodeInsightFixture.findFileInTempDir("../" + module.getName() + ".iml");
    String config = VfsUtilCore.loadText(imlFile);

    String dirName = PathUtil.getFileName(myCodeInsightFixture.getTempDirFixture().getTempDirPath());
    assertThat(config).contains("<setting name=\"additionalCompilerParameters\" value=\"-additionalParam $MODULE_DIR$/" + dirName + "/empty.txt\"");
    assertThat(config).contains("<setting name=\"compilerParameters\" value=\"-param $MODULE_DIR$/" + dirName + "/empty.txt\"");
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "model/configuration";
  }
}
