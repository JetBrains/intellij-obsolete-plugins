package com.intellij.dmserver.facet;

import com.intellij.facet.FacetTypeId;
import com.intellij.facet.ui.FacetBasedFrameworkSupportProvider;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportConfigurable;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModelAdapter;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportProvider;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.spring.facet.SpringFacet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DMModuleBundleFacetSettingsPanel implements DMModuleFacetSettingsPanel<DMBundleFacetConfiguration> {
  private JCheckBox mySpringDMSupportCheckBox;
  private JPanel myMainPanel;
  private JCheckBox myWebModuleCheckBox;
  private JPanel myWebModulePanel;

  private DMWebFacetFrameworkSupportProvider myWebSupportProvider;
  private FrameworkSupportConfigurable myWebSupportConfigurable;
  private String myWebVersionName;
  private boolean myIsCreateWebConfigFile;

  private Module myConfiguredModule;
  private ModulesProvider myModulesProvider;

  private boolean mySpringSupportUpdated;
  private boolean myWebSupportUpdated;

  @Override
  public void init(@Nullable Project project,
                   @Nullable Module configuredModule,
                   @NotNull ModulesProvider modulesProvider,
                   @NotNull Disposable parentDisposable) {
    myConfiguredModule = configuredModule;
    myModulesProvider = modulesProvider;
    mySpringDMSupportCheckBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        mySpringSupportUpdated = true;
      }
    });
    myWebModuleCheckBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        myWebSupportUpdated = true;
      }
    });
  }

  public void initFrameworkContribution(DMWebFacetFrameworkSupportProvider webSupportProvider, final FrameworkSupportModel model) {
    final String springProviderId = FacetBasedFrameworkSupportProvider.getProviderId(SpringFacet.FACET_TYPE_ID);

    mySpringDMSupportCheckBox.setSelected(model.isFrameworkSelected(springProviderId));

    model.addFrameworkListener(new FrameworkSupportModelAdapter() {

      boolean myInSpringSelectionUpdate = false;

      @Override
      public void frameworkSelected(@NotNull FrameworkSupportProvider provider) {
        onFrameworkSelectionChanged(provider, true);
      }

      @Override
      public void frameworkUnselected(@NotNull FrameworkSupportProvider provider) {
        onFrameworkSelectionChanged(provider, false);
      }

      private void onFrameworkSelectionChanged(FrameworkSupportProvider provider, boolean selected) {
        if (springProviderId.equals(provider.getId()) && !myInSpringSelectionUpdate) {
          try {
            myInSpringSelectionUpdate = true;
            mySpringDMSupportCheckBox.setSelected(selected);
          }
          finally {
            myInSpringSelectionUpdate = false;
          }
        }
      }
    });
    mySpringDMSupportCheckBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        model.setFrameworkComponentEnabled(springProviderId, mySpringDMSupportCheckBox.isSelected());
      }
    });

    myWebSupportProvider = webSupportProvider;
    myWebSupportConfigurable = webSupportProvider.createConfigurable(model);
    myWebModulePanel.add(myWebSupportConfigurable.getComponent());
    myWebModuleCheckBox.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        updateEnablement();
      }
    });
  }

  @Override
  @NotNull
  public JPanel getMainPanel() {
    return myMainPanel;
  }

  @Override
  public void load(@NotNull DMBundleFacetConfiguration configuration) {
    mySpringDMSupportCheckBox.setSelected(configuration.getIsSpringDM());
    myWebModuleCheckBox.setSelected(configuration.getIsWebModule());
    myWebVersionName = configuration.getWebFrameworkVersionName();
    myIsCreateWebConfigFile = configuration.getIsCreateWebConfigFile();
    mySpringSupportUpdated = false;
    myWebSupportUpdated = false;
  }

  private boolean getSupportSelection(boolean selectionUpdated, boolean uiSelection, FacetTypeId<?> facetTypeId) {
    return selectionUpdated || myConfiguredModule == null
           ? uiSelection
           : !myModulesProvider.getFacetModel(myConfiguredModule).getFacetsByType(facetTypeId).isEmpty();
  }

  @Override
  public void apply(@NotNull DMBundleFacetConfiguration configuration) {
    doSave(configuration,
           getSupportSelection(mySpringSupportUpdated,
                               mySpringDMSupportCheckBox.isSelected(),
                               SpringFacet.FACET_TYPE_ID),
           getSupportSelection(myWebSupportUpdated,
                               myWebModuleCheckBox.isSelected(),
                               WebFacet.ID));
    mySpringSupportUpdated = false;
    myWebSupportUpdated = false;
  }

  @Override
  public void save(@NotNull DMBundleFacetConfiguration configuration) {
    doSave(configuration,
           mySpringDMSupportCheckBox.isSelected(),
           myWebModuleCheckBox.isSelected());
  }

  private void doSave(@NotNull DMBundleFacetConfiguration configuration,
                      boolean springSupportSelection,
                      boolean webSupportSelection) {
    configuration.setIsSpringDM(springSupportSelection);
    configuration.setIsWebModule(webSupportSelection);

    configuration.setWebFrameworkVersionName(myWebSupportProvider == null //
                                             ? myWebVersionName //
                                             : myWebSupportConfigurable.getSelectedVersion().getVersionName());
    configuration.setIsCreateWebConfigFile(myWebSupportProvider == null //
                                           ? myIsCreateWebConfigFile //
                                           : myWebSupportProvider
                                             .isCreateConfigFile(myWebSupportConfigurable.getSelectedVersion().getVersionName()));
  }

  @Override
  public void updateEnablement() {
    setEnabledRecursively(myWebModulePanel, myWebModuleCheckBox.isSelected());
  }

  private static void setEnabledRecursively(Component component, boolean enabled) {
    component.setEnabled(enabled);
    if (component instanceof Container) {
      for (Component child : ((Container)component).getComponents()) {
        setEnabledRecursively(child, enabled);
      }
    }
  }
}
