package com.intellij.dmserver.facet;

import com.intellij.dmserver.install.DMServerInstallation;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportConfigurable;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.ui.configuration.DefaultModulesProvider;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.startup.StartupManager;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DMServerSupportConfigurable extends FrameworkSupportConfigurable {
  private final FrameworkSupportModel myModel;
  private DMProjectFacetSettingsPanel myProjectSettingsPanel;
  private JRadioButton myBundleRadioButton;
  private JRadioButton myCompositeRadioButton;
  private JRadioButton myConfigRadioButton;
  private JPanel myMainPanel;
  private JPanel myFacetSettingsPanel;

  private final DMServerSupportProvider myFrameworkSupportProvider;
  private final boolean myInProjectCreation;

  private final FacetContribution[] myFacetContributions = new FacetContribution[]{
        new BundleFacetContribution(),
        new CompositeFacetContribution(),
        new ConfigFacetContribution()};

  private final Project myProject;
  private final ModulesProvider myModulesProvider;

  public DMServerSupportConfigurable(@NotNull FrameworkSupportModel model, @NotNull DMServerSupportProvider frameworkSupportProvider) {
    myProject = model.getProject();
    myFrameworkSupportProvider = frameworkSupportProvider;
    myInProjectCreation = myProject == null;

    myProjectSettingsPanel.init(myProject);

    myModulesProvider = DefaultModulesProvider.createForProject(myProject);
    myModel = model;
    for (final FacetContribution contribution : myFacetContributions) {
      contribution.getRadioButton().addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          selectContribution(contribution);
        }
      });
    }
    myBundleRadioButton.setSelected(true);
    selectContribution(myFacetContributions[0]);
  }

  public void selectContribution(FacetContribution contribution) {
    contribution.getPanel().updateEnablement();
    ((CardLayout)myFacetSettingsPanel.getLayout()).show(myFacetSettingsPanel, contribution.getCardName());
    myFacetSettingsPanel.repaint();
  }

  @Override
  public JComponent getComponent() {
    return myMainPanel;
  }

  @Override
  public void addSupport(final @NotNull Module module, @NotNull ModifiableRootModel model, @Nullable Library library) {
    if (myInProjectCreation) {
      StartupManager.getInstance(module.getProject()).runWhenProjectIsInitialized(
        () -> myProjectSettingsPanel.applyFrameworkSelection(module.getProject()));
    }

    getSelectedFacetSupport().addSupport(module, model);
  }

  public void finishAddSupport(@NotNull Module module, @NotNull ModifiableRootModel model) {
    getSelectedFacetSupport().finishAddSupport(module, model, myProjectSettingsPanel.getSelectedServerInstallation());
  }

  private FacetContribution getSelectedFacetSupport() {
    for (FacetContribution facetContribution : myFacetContributions) {
      if (facetContribution.isSelected()) {
        return facetContribution;
      }
    }
    throw new IllegalStateException();
  }

  @Override
  public void onFrameworkSelectionChanged(boolean selected) {
    if (selected) {
      getSelectedFacetSupport().getPanel().updateEnablement();
    }
  }

  private abstract class FacetContribution<C extends DMFacetConfigurationBase<C>, P extends DMFacetSupportProviderBase<?, C>> {

    private DMModuleFacetSettingsPanel<C> myPanel;

    public void init() {
      myPanel.init(myProject, null, myModulesProvider, DMServerSupportConfigurable.this);
      myPanel.load(createFacetConfiguration());
    }

    public DMModuleFacetSettingsPanel<C> getPanel() {
      if (myPanel == null) {
        myPanel = createModuleSettingsPanel();
        myFacetSettingsPanel.add(getCardName(), myPanel.getMainPanel());
        init();
      }
      return myPanel;
    }

    public boolean isSelected() {
      return getRadioButton().isSelected();
    }

    public void addSupport(@NotNull Module module, @NotNull ModifiableRootModel rootModel) {
      getProvider().addDMSupport(module, rootModel);
    }

    public void finishAddSupport(Module module, ModifiableRootModel rootModel, DMServerInstallation installation) {
      C facetConfiguration = createFacetConfiguration();
      myPanel.save(facetConfiguration);
      getProvider().finishAddDMSupport(module, rootModel, installation, facetConfiguration);
    }

    protected abstract C createFacetConfiguration();

    protected abstract DMModuleFacetSettingsPanel<C> createModuleSettingsPanel();

    protected abstract JRadioButton getRadioButton();

    @NonNls
    protected abstract String getCardName();

    protected abstract P getProvider();
  }

  private class BundleFacetContribution extends FacetContribution<DMBundleFacetConfiguration, DMBundleSupportProvider> {

    private DMModuleBundleFacetSettingsPanel myFacetSettingsPanel;

    @Override
    public void init() {
      super.init();
      myFacetSettingsPanel.initFrameworkContribution(getProvider().getWebSupportProvider(), myModel);
    }

    @Override
    protected DMBundleFacetConfiguration createFacetConfiguration() {
      return new DMBundleFacetConfiguration();
    }

    @Override
    protected DMModuleFacetSettingsPanel<DMBundleFacetConfiguration> createModuleSettingsPanel() {
      myFacetSettingsPanel = new DMModuleBundleFacetSettingsPanel();
      return myFacetSettingsPanel;
    }

    @Override
    protected JRadioButton getRadioButton() {
      return myBundleRadioButton;
    }

    @NonNls
    @Override
    protected String getCardName() {
      return "bundle";
    }

    @Override
    protected DMBundleSupportProvider getProvider() {
      return myFrameworkSupportProvider.getBundleSupportProvider();
    }
  }

  private class CompositeFacetContribution extends FacetContribution<DMCompositeFacetConfiguration, DMCompositeSupportProvider> {

    @Override
    protected DMCompositeFacetConfiguration createFacetConfiguration() {
      return new DMCompositeFacetConfiguration();
    }

    @Override
    protected DMModuleFacetSettingsPanel<DMCompositeFacetConfiguration> createModuleSettingsPanel() {
      return new DMModuleCompositeFacetSettingsPanel();
    }

    @Override
    protected JRadioButton getRadioButton() {
      return myCompositeRadioButton;
    }

    @NonNls
    @Override
    protected String getCardName() {
      return "composite";
    }

    @Override
    protected DMCompositeSupportProvider getProvider() {
      return myFrameworkSupportProvider.getCompositeSupportProvider();
    }
  }

  private class ConfigFacetContribution extends FacetContribution<DMConfigFacetConfiguration, DMConfigSupportProvider> {

    @Override
    protected DMConfigFacetConfiguration createFacetConfiguration() {
      return new DMConfigFacetConfiguration();
    }

    @Override
    protected DMModuleFacetSettingsPanel<DMConfigFacetConfiguration> createModuleSettingsPanel() {
      return new DMModuleConfigFacetSettingsPanel();
    }

    @Override
    protected JRadioButton getRadioButton() {
      return myConfigRadioButton;
    }

    @NonNls
    @Override
    protected String getCardName() {
      return "config";
    }

    @Override
    protected DMConfigSupportProvider getProvider() {
      return myFrameworkSupportProvider.getConfigSupportProvider();
    }
  }
}
