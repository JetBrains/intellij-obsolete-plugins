package com.intellij.dmserver.facet;

import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DMConfigFacetConfiguration extends DMFacetConfigurationBase<DMConfigFacetConfiguration> {

  private String myName = "";

  @Override
  public FacetEditorTab[] createEditorTabs(FacetEditorContext facetEditorContext, FacetValidatorsManager facetValidatorsManager) {
    return new FacetEditorTab[]{
      new DMConfigFacetEditor(facetEditorContext, this) //
    };
  }

  @Override
  public void loadState(@NotNull DMConfigFacetConfiguration state) {
    setName(state.getName());
  }

  public String getName(@Nullable Module module) {
    String result = getName();
    return module == null || !StringUtil.isEmpty(result) ? result : module.getName();
  }

  public String getName() {
    return myName;
  }

  public void setName(String name) {
    myName = name;
  }

  @Override
  public DMConfigFacetConfiguration getState() {
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (!super.equals(obj)) {
      return false;
    }
    if (!(obj instanceof DMConfigFacetConfiguration)) {
      return false;
    }
    DMConfigFacetConfiguration asInstance = (DMConfigFacetConfiguration)obj;
    if (!asInstance.getName().equals(getName())) {
      return false;
    }
    return true;
  }
}
