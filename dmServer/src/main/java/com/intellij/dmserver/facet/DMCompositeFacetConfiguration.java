package com.intellij.dmserver.facet;

import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.xmlb.annotations.AbstractCollection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

public class DMCompositeFacetConfiguration extends DMFacetConfigurationBase<DMCompositeFacetConfiguration> {

  private String myName = "";
  private String myVersion = "1.0.0";
  private boolean myScoped = false;
  private boolean myIsAtomic = false;

  private final ArrayList<NestedUnitIdentity> myNestedBundles = new ArrayList<>();

  private DMCompositeType myCompositeType = DMCompositeType.PLAN;

  @Override
  public FacetEditorTab[] createEditorTabs(FacetEditorContext facetEditorContext, FacetValidatorsManager facetValidatorsManager) {
    return new FacetEditorTab[]{
      new DMCompositeFacetEditor(facetEditorContext, this) //
    };
  }

  @AbstractCollection(elementTag = "nestedUnits")
  public Collection<NestedUnitIdentity> getNestedBundles() {
    return new ArrayList<>(myNestedBundles);
  }

  public void setNestedBundles(Collection<NestedUnitIdentity> nestedBundles) {
    myNestedBundles.clear();
    myNestedBundles.addAll(nestedBundles);
  }

  @Override
  public void loadState(@NotNull DMCompositeFacetConfiguration state) {
    setNestedBundles(state.getNestedBundles());
    setName(state.getName());
    setVersion(state.getVersion());
    setScoped(state.getScoped());
    setAtomic(state.getAtomic());
    setCompositeType(state.getCompositeType());
  }

  public String getName() {
    return myName;
  }

  public String getName(@Nullable Module module) {
    String result = getName();
    return module == null || !StringUtil.isEmpty(result) ? result : module.getName();
  }

  public void setName(String name) {
    myName = name;
  }

  public boolean getScoped() {
    return myScoped;
  }

  public void setScoped(boolean isScoped) {
    myScoped = isScoped;
  }

  public boolean getAtomic() {
    return myIsAtomic;
  }

  public void setAtomic(boolean isAtomic) {
    myIsAtomic = isAtomic;
  }

  public String getVersion() {
    return myVersion;
  }

  public void setVersion(String version) {
    myVersion = version;
  }

  public DMCompositeType getCompositeType() {
    return myCompositeType;
  }

  public void setCompositeType(DMCompositeType compositeType) {
    myCompositeType = compositeType;
  }

  @Override
  public DMCompositeFacetConfiguration getState() {
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (!super.equals(obj)) {
      return false;
    }
    if (!(obj instanceof DMCompositeFacetConfiguration)) {
      return false;
    }
    DMCompositeFacetConfiguration asInstance = (DMCompositeFacetConfiguration)obj;
    if (!asInstance.getNestedBundles().equals(getNestedBundles())) {
      return false;
    }
    if (!asInstance.getName().equals(getName())) {
      return false;
    }
    if (!asInstance.getVersion().equals(getVersion())) {
      return false;
    }
    if (asInstance.getScoped() != getScoped()) {
      return false;
    }
    if (asInstance.getAtomic() != getAtomic()) {
      return false;
    }
    if (asInstance.getCompositeType() != getCompositeType()) {
      return false;
    }
    return true;
  }

  public void init(Project project) {
    for (NestedUnitIdentity unitIdentity : getNestedBundles()) {
      unitIdentity.init(project);
    }
  }
}
