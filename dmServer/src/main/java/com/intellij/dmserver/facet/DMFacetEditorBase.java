package com.intellij.dmserver.facet;

import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public abstract class DMFacetEditorBase<T extends DMFacetBase<C>, C extends DMFacetConfigurationBase<C>> extends FacetEditorTab
  implements Disposable {

  private final FacetEditorContext myFacetEditorContext;
  private final C myFacetConfiguration;

  private final DMModuleFacetSettingsPanel<C> mySettingsPanel;

  public DMFacetEditorBase(FacetEditorContext facetEditorContext, C facetConfiguration, DMModuleFacetSettingsPanel<C> settingsPanel) {
    myFacetEditorContext = facetEditorContext;
    myFacetConfiguration = facetConfiguration;
    mySettingsPanel = settingsPanel;
    mySettingsPanel.init(facetEditorContext.getProject(), facetEditorContext.getModule(), facetEditorContext.getModulesProvider(), this);
  }

  protected final FacetEditorContext getFacetEditorContext() {
    return myFacetEditorContext;
  }

  @Override
  @NotNull
  public JComponent createComponent() {
    return mySettingsPanel.getMainPanel();
  }

  @Override
  public void disposeUIResources() {
    Disposer.dispose(this);
  }

  @Override
  public void dispose() {

  }

  @Override
  public void apply() throws ConfigurationException {
    mySettingsPanel.apply(myFacetConfiguration);

    T facet = (T)myFacetEditorContext.getFacet();
    facet.getCommonPart().fireFacetChanged();
    facet.updateSupportWithArtifact(myFacetEditorContext.getModifiableRootModel(), myFacetEditorContext.getModulesProvider());
  }

  @Override
  public boolean isModified() {
    C uiConfiguration = createFacetConfiguration();
    mySettingsPanel.save(uiConfiguration);
    return !myFacetConfiguration.equals(uiConfiguration);
  }

  @Override
  public void reset() {
    mySettingsPanel.load(myFacetConfiguration);
  }

  protected abstract C createFacetConfiguration();
}
