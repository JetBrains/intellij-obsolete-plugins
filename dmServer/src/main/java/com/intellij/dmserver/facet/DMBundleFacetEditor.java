package com.intellij.dmserver.facet;

import com.intellij.facet.impl.ProjectFacetsConfigurator;
import com.intellij.facet.impl.ui.FacetEditorImpl;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.roots.ui.configuration.FacetsProvider;
import org.jetbrains.annotations.Nls;
import org.osmorc.facet.OsmorcFacet;

public class DMBundleFacetEditor extends DMFacetEditorBase<DMBundleFacet, DMBundleFacetConfiguration> {

  public DMBundleFacetEditor(FacetEditorContext facetEditorContext, DMBundleFacetConfiguration facetConfiguration) {
    super(facetEditorContext, facetConfiguration, new DMModuleBundleFacetSettingsPanel());
  }

  @Override
  @Nls
  public String getDisplayName() {
    return DMBundleFacetType.getDisplayName();
  }

  @Override
  protected DMBundleFacetConfiguration createFacetConfiguration() {
    return new DMBundleFacetConfiguration();
  }

  @Override
  public void apply() throws ConfigurationException {
    new OsmorcFacetEditorProcessor() {

      @Override
      protected void doProcess(FacetEditorImpl osmorcFacetEditor) throws ConfigurationException {
        for (FacetEditorTab editorTab : osmorcFacetEditor.getEditorTabs()) {
          editorTab.apply();
        }
      }
    }.process();

    super.apply();

    new OsmorcFacetEditorProcessor() {

      @Override
      protected void doProcess(FacetEditorImpl osmorcFacetEditor) {
        for (FacetEditorTab editorTab : osmorcFacetEditor.getEditorTabs()) {
          editorTab.reset();
        }
      }
    }.process();
  }

  private abstract class OsmorcFacetEditorProcessor {

    public void process() throws ConfigurationException {
      OsmorcFacet osmorcFacet = (OsmorcFacet)getFacetEditorContext().getFacet().getUnderlyingFacet();

      FacetsProvider facetsProvider = getFacetEditorContext().getFacetsProvider();
      if (!(facetsProvider instanceof ProjectFacetsConfigurator)) {
        return;
      }
      ProjectFacetsConfigurator projectFacetsConfigurator = (ProjectFacetsConfigurator)facetsProvider;
      FacetEditorImpl osmorcFacetEditor = projectFacetsConfigurator.getEditor(osmorcFacet);
      if (osmorcFacetEditor == null) {
        return;
      }

      doProcess(osmorcFacetEditor);
    }

    protected abstract void doProcess(FacetEditorImpl osmorcFacetEditor) throws ConfigurationException;
  }

}
