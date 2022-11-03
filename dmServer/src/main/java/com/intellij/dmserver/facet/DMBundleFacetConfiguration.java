package com.intellij.dmserver.facet;

import com.intellij.facet.FacetTypeId;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.spring.facet.SpringFacet;
import com.intellij.util.xmlb.annotations.Tag;
import org.jetbrains.annotations.NotNull;

public class DMBundleFacetConfiguration extends DMFacetConfigurationBase<DMBundleFacetConfiguration> {

  private boolean myIsDynamicModules = false;
  private boolean myIsWebModule = false;
  private String myWebFrameworkVersionName;
  private boolean myIsCreateWebConfigFile = true;

  private static boolean hasFacet(FacetEditorContext facetEditorContext, FacetTypeId<?> facetTypeId) {
    return !facetEditorContext.getFacetsProvider().getFacetsByType(facetEditorContext.getModule(), facetTypeId).isEmpty();
  }

  @Override
  public FacetEditorTab[] createEditorTabs(FacetEditorContext facetEditorContext, FacetValidatorsManager facetValidatorsManager) {
    myIsDynamicModules = hasFacet(facetEditorContext, SpringFacet.FACET_TYPE_ID);
    myIsWebModule = hasFacet(facetEditorContext, WebFacet.ID);
    return new FacetEditorTab[]{
      new DMBundleFacetEditor(facetEditorContext, this) //
    };
  }

  @Override
  public void loadState(@NotNull DMBundleFacetConfiguration state) {
    setIsSpringDM(state.getIsSpringDM());
    setIsWebModule(state.getIsWebModule());
    setWebFrameworkVersionName(state.getWebFrameworkVersionName());
    setIsCreateWebConfigFile(state.getIsCreateWebConfigFile());
  }

  @Override
  public DMBundleFacetConfiguration getState() {
    return this;
  }

  @Tag("springDM")
  public boolean getIsSpringDM() {
    return myIsDynamicModules;
  }

  public void setIsSpringDM(boolean isSpringDM) {
    myIsDynamicModules = isSpringDM;
  }

  @Tag("web")
  public boolean getIsWebModule() {
    return myIsWebModule;
  }

  public void setIsWebModule(boolean isWebModule) {
    myIsWebModule = isWebModule;
  }

  @Tag("webVersion")
  public String getWebFrameworkVersionName() {
    return myWebFrameworkVersionName;
  }

  public void setWebFrameworkVersionName(String webFrameworkVersionName) {
    myWebFrameworkVersionName = webFrameworkVersionName;
  }

  @Tag("createWebConfigFile")
  public boolean getIsCreateWebConfigFile() {
    return myIsCreateWebConfigFile;
  }

  public void setIsCreateWebConfigFile(boolean isCreateWebConfigFile) {
    myIsCreateWebConfigFile = isCreateWebConfigFile;
  }

  @Override
  public boolean equals(Object obj) {
    if (!super.equals(obj)) {
      return false;
    }
    if (!(obj instanceof DMBundleFacetConfiguration)) {
      return false;
    }
    DMBundleFacetConfiguration asInstance = (DMBundleFacetConfiguration)obj;
    if (asInstance.getIsSpringDM() != getIsSpringDM()) {
      return false;
    }
    if (asInstance.getIsWebModule() != getIsWebModule()) {
      return false;
    }
    if (!StringUtil.notNullize(asInstance.getWebFrameworkVersionName()).equals(StringUtil.notNullize(getWebFrameworkVersionName()))) {
      return false;
    }
    if (asInstance.getIsCreateWebConfigFile() != getIsCreateWebConfigFile()) {
      return false;
    }
    return true;
  }
}
