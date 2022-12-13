package com.intellij.dmserver.facet;

import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.dmserver.util.IconUtils;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ChooseModulesDialog;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.Ref;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.util.PlatformIcons;
import com.intellij.util.containers.ContainerUtil;
import icons.DmServerSupportIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.*;

public class DMModuleCompositeFacetSettingsPanel implements DMModuleFacetSettingsPanel<DMCompositeFacetConfiguration> {

  private DefaultListModel<NestedUnitIdentity> myNestedBundlesListModel;

  private JPanel myMainPanel;
  private JRadioButton myPlanRadio;
  private JRadioButton myPlatformArchiveRadio;
  private JButton myAddBundleButton;
  private JButton myRemoveBundleButton;
  private JList<NestedUnitIdentity> myNestedBundlesList;
  private JLabel myTotalBundlesCountLabel;
  private JPanel myNestedBundlesGroup;
  private JPanel myRadioPanel;
  private JPanel myPlanFlagsPanel;
  private JCheckBox myAtomicCheckBox;
  private JCheckBox myScopedCheckBox;
  private JTextField myVersionTextField;
  private JPanel myVersionPanel;
  private JTextField myNameTextField;
  private JPanel myNamePanel;
  private JButton myVersionsButton;
  private JButton myMoveUpButton;
  private JButton myMoveDownButton;
  private JLabel myWarningLabel;

  private final Set<Module> myAvailableNestedModules = new TreeSet<>(Comparator.comparing(Module::getName));

  private boolean myInitialized = false;

  private Project myProject;
  private NestedUnitProvider myNestedUnitProvider;

  private Behavior myBehavior;

  private Map<DMCompositeType, Behavior> myType2Behavior;

  private Module myConfiguredModule;

  @Override
  public void init(@Nullable Project project,
                   @Nullable Module configuredModule,
                   @NotNull ModulesProvider modulesProvider,
                   @NotNull Disposable parentDisposable) {
    myConfiguredModule = configuredModule;
    if (myInitialized) {
      throw new RuntimeException("Should be called once");
    }
    myInitialized = true;

    myProject = project;
    myNestedUnitProvider = new NestedUnitProvider(configuredModule, modulesProvider);

    ButtonGroup radioGroup = new ButtonGroup();
    radioGroup.add(myPlanRadio);
    radioGroup.add(myPlatformArchiveRadio);

    Behavior[] behaviors = new Behavior[]{new PlanBehavior(), new ParBehavior()};
    myType2Behavior = new HashMap<>();
    for (final Behavior behavior : behaviors) {
      myType2Behavior.put(behavior.getType(), behavior);
      behavior.getRadioButton().addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          setBehavior(behavior.getType());
        }
      });
    }

    myScopedCheckBox.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        updateAvailableNestedModules();
      }
    });

    myNestedBundlesListModel = new DefaultListModel<>();
    myNestedBundlesList.setModel(myNestedBundlesListModel);
    myNestedBundlesList.setCellRenderer(SimpleListCellRenderer.create((label, value, index) -> {
      Module module = value.getModule();
      label.setText(myBehavior.getNestedUnitIdentityText(value));
      label.setIcon(getModuleIcon(module));
    }));
    myNestedBundlesList.addListSelectionListener(new ListSelectionListener() {

      @Override
      public void valueChanged(ListSelectionEvent e) {
        updateEnablement();
      }
    });

    setBehavior(DMCompositeType.PLAN);

    myAddBundleButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        List<Module> toAdd = chooseNestedModules();
        List<NestedUnitIdentity> identitiesToAdd = new ArrayList<>();
        for (Module module : toAdd) {
          NestedUnitIdentity unitIdentity = new NestedUnitIdentity(module);
          myNestedBundlesListModel.addElement(unitIdentity);
          identitiesToAdd.add(unitIdentity);
        }
        int[] addedIndices = new int[toAdd.size()];
        int nextPosition = 0;
        for (NestedUnitIdentity identity : identitiesToAdd) {
          addedIndices[nextPosition++] = myNestedBundlesListModel.indexOf(identity);
        }
        myNestedBundlesList.setSelectedIndices(addedIndices);
        nestedBundlesSetChanged();
      }
    });

    myRemoveBundleButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        for (Object next : myNestedBundlesList.getSelectedValuesList()) {
          myNestedBundlesListModel.removeElement(next);
        }
        myNestedBundlesList.setSelectedIndices(new int[0]);
        nestedBundlesSetChanged();
      }
    });

    myVersionsButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        onVersions();
      }
    });

    myMoveUpButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        onMoveUp();
      }
    });

    myMoveDownButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        onMoveDown();
      }
    });

    IconUtils.setupWarningLabel(myWarningLabel);

    if (project != null) {
      new DMNestedBundlesUpdater(project, parentDisposable) {

        @Override
        protected Collection<NestedUnitIdentity> getNestedBundles() {
          return getNestedModuleIdentities();
        }

        @Override
        protected void setNestedBundles(Collection<NestedUnitIdentity> nestedBundles) {
          setNestedModuleIdentities(nestedBundles);
        }

        @Override
        protected void dmFacetAddedOrRemoved(DMFacetBase facet) {
          updateAvailableNestedModules();
        }
      };
    }
  }

  private Project getProject() {
    return myProject;
  }

  private void setBehavior(DMCompositeType type) {
    myBehavior = myType2Behavior.get(type);
    updateAvailableNestedModules();
  }

  @Override
  public void load(@NotNull DMCompositeFacetConfiguration configuration) {
    myType2Behavior.get(configuration.getCompositeType()).getRadioButton().setSelected(true);
    setBehavior(configuration.getCompositeType());
    myNameTextField.setText(configuration.getName(myConfiguredModule));
    myVersionTextField.setText(configuration.getVersion());
    myScopedCheckBox.setSelected(myBehavior.transformScoped(configuration.getScoped()));
    myAtomicCheckBox.setSelected(myBehavior.transformAtomic(configuration.getAtomic()));
    setNestedModuleIdentities(configuration.getNestedBundles());
  }

  @Override
  public void apply(@NotNull DMCompositeFacetConfiguration configuration) {
    save(configuration);
  }

  @Override
  public void save(@NotNull DMCompositeFacetConfiguration configuration) {
    configuration.setCompositeType(myBehavior.getType());
    configuration.setName(myNameTextField.getText());
    configuration.setVersion(myVersionTextField.getText());
    configuration.setScoped(myBehavior.transformScoped(myScopedCheckBox.isSelected()));
    configuration.setAtomic(myBehavior.transformAtomic(myAtomicCheckBox.isSelected()));
    configuration.setNestedBundles(getNestedModuleIdentities());
  }

  private void onMoveUp() {
    if (myNestedBundlesList.getSelectedIndices().length != 1) {
      return; // should never happen
    }
    int selectedIndex = myNestedBundlesList.getSelectedIndex();
    if (selectedIndex == 0) {
      return; // should never happen
    }
    NestedUnitIdentity selectedElement = myNestedBundlesListModel.getElementAt(selectedIndex);
    myNestedBundlesListModel.removeElementAt(selectedIndex);
    myNestedBundlesListModel.insertElementAt(selectedElement, selectedIndex - 1);
    myNestedBundlesList.setSelectedIndex(selectedIndex - 1);
  }

  private void onMoveDown() {
    if (myNestedBundlesList.getSelectedIndices().length != 1) {
      return; // should never happen
    }
    int selectedIndex = myNestedBundlesList.getSelectedIndex();
    if (selectedIndex == myNestedBundlesListModel.size() - 1) {
      return; // should never happen
    }
    NestedUnitIdentity selectedElement = myNestedBundlesListModel.getElementAt(selectedIndex);
    myNestedBundlesListModel.insertElementAt(selectedElement, selectedIndex + 2);
    myNestedBundlesListModel.removeElementAt(selectedIndex);
    myNestedBundlesList.setSelectedIndex(selectedIndex + 1);
  }

  private void onVersions() {
    List<NestedUnitIdentity> unitPlanIdentities = new ArrayList<>(myNestedBundlesList.getSelectedValuesList());
    VersionsRangeDialog versionsRangeDialog = new VersionsRangeDialog(myProject, unitPlanIdentities);
    if (versionsRangeDialog.showAndGet()) {
      myNestedBundlesList.updateUI();
    }
  }

  @Override
  @NotNull
  public JPanel getMainPanel() {
    return myMainPanel;
  }

  private Set<Module> getNestedModules() {
    Set<Module> result = new HashSet<>();
    for (NestedUnitIdentity unitIdentity : getNestedModuleIdentities()) {
      ContainerUtil.addIfNotNull(result, unitIdentity.getModule());
    }
    return result;
  }

  private void updateAvailableNestedModules() {
    myAvailableNestedModules.clear();
    List<Module> possibleNestedModules = myNestedUnitProvider.getPossibleNestedModules(getEditedConfiguration());
    Set<Module> nestedModules = getNestedModules();
    for (Module possibleNestedModule : possibleNestedModules) {
      if (!nestedModules.contains(possibleNestedModule)) {
        myAvailableNestedModules.add(possibleNestedModule);
      }
    }
    myNestedBundlesList.updateUI();
    updateWarningLabel();
    updateEnablement();
  }

  private void updateWarningLabel() {
    boolean hasImpossibleNestedModules = false;
    Ref<String> errorRef = new Ref<>();
    Module impossibleModule = null;
    for (Module module : getNestedModules()) {
      if (!NestedUnitProvider.isPossibleNestedModule(module, getEditedConfiguration(), errorRef)) {
        hasImpossibleNestedModules = true;
        impossibleModule = module;
        break;
      }
    }

    if (hasImpossibleNestedModules) {
      myWarningLabel
        .setText(DmServerBundle.message("DMModuleCompositeFacetSettingsPanel.error.module", impossibleModule.getName(), errorRef.get()));
    }
    myWarningLabel.setVisible(hasImpossibleNestedModules);
  }

  private DMCompositeFacetConfiguration getEditedConfiguration() {
    DMCompositeFacetConfiguration result = new DMCompositeFacetConfiguration();
    save(result);
    return result;
  }

  private List<Module> chooseNestedModules() {
    if (myAvailableNestedModules.isEmpty()) {
      return Collections.emptyList();
    }
    ChooseModulesDialog dialog = new ChooseModulesDialog(getProject(), new ArrayList<>(myAvailableNestedModules),
                                                         myBehavior.getChooseModulesDialogTitle(), null) {

      @Override
      protected Icon getItemIcon(Module item) {
        return getModuleIcon(item);
      }
    };
    return dialog.showAndGet() ? dialog.getChosenElements() : Collections.emptyList();
  }


  public Set<NestedUnitIdentity> getNestedModuleIdentities() {
    Set<NestedUnitIdentity> result = myBehavior.createNestedModuleIdentitiesSet();
    for (int i = 0; i < myNestedBundlesListModel.getSize(); i++) {
      result.add(myNestedBundlesListModel.getElementAt(i));
    }
    return result;
  }


  public void setNestedModuleIdentities(Collection<NestedUnitIdentity> nestedUnitIdentities) {
    myNestedBundlesListModel.clear();
    for (NestedUnitIdentity unitIdentity : nestedUnitIdentities) {
      Module nextModule = unitIdentity.getModule();
      if (nextModule == null) {
        continue;
      }
      myNestedBundlesListModel.addElement(unitIdentity.clone());
    }
    nestedBundlesSetChanged();
  }


  private void nestedBundlesSetChanged() {
    myTotalBundlesCountLabel
      .setText(DmServerBundle.message("DMModuleCompositeFacetSettingsPanel.label.total.bundles", myNestedBundlesListModel.getSize()));
    updateAvailableNestedModules();
  }


  private static Icon getModuleIcon(Module module) {
    Icon result;
    if (module == null) {
      result = null;
    }
    else {
      result = new DMFacetsSwitch<Icon>() {

        @Override
        protected Icon doProcessBundleFacet(DMBundleFacet bundleFacet) {
          return DmServerSupportIcons.Bundle;
        }

        @Override
        protected Icon doProcessCompositeFacet(DMCompositeFacet compositeFacet) {
          return switch (compositeFacet.getConfigurationImpl().getCompositeType()) {
            case PAR -> DmServerSupportIcons.ParBundle;
            case PLAN -> DmServerSupportIcons.DM;
          };
        }

        @Override
        protected Icon doProcessConfigFacet(DMConfigFacet configFacet) {
          return AllIcons.FileTypes.Text;
        }
      }.processModule(module);
    }

    if (result == null) {
      result = PlatformIcons.ERROR_INTRODUCTION_ICON;
    }

    return result;
  }

  @Override
  public void updateEnablement() {
    myBehavior.updateEnablement();
  }

  private abstract class Behavior {

    public abstract DMCompositeType getType();

    public abstract JRadioButton getRadioButton();

    public void updateEnablement() {
      myAddBundleButton.setEnabled(!myAvailableNestedModules.isEmpty());
      myRemoveBundleButton.setEnabled(myNestedBundlesList.getSelectedIndex() >= 0);
    }

    @Nls
    protected abstract String getChooseModulesDialogTitle();

    @Nls
    public abstract String getNestedUnitIdentityText(NestedUnitIdentity unitIdentity);

    public abstract Set<NestedUnitIdentity> createNestedModuleIdentitiesSet();

    public abstract boolean transformScoped(boolean scoped);

    public abstract boolean transformAtomic(boolean atomic);
  }

  private class ParBehavior extends Behavior {

    @Override
    public DMCompositeType getType() {
      return DMCompositeType.PAR;
    }

    @Override
    public JRadioButton getRadioButton() {
      return myPlatformArchiveRadio;
    }

    @Override
    public void updateEnablement() {
      myScopedCheckBox.setEnabled(false);
      myAtomicCheckBox.setEnabled(false);
      myVersionsButton.setEnabled(false);
      myMoveUpButton.setEnabled(false);
      myMoveDownButton.setEnabled(false);
      super.updateEnablement();
    }

    @Override
    public boolean transformAtomic(boolean atomic) {
      return true;
    }

    @Override
    public boolean transformScoped(boolean scoped) {
      return true;
    }

    @Override
    @Nls
    protected String getChooseModulesDialogTitle() {
      return DmServerBundle.message("DMModuleCompositeFacetSettingsPanel.ParBehavior.chooser.title");
    }

    @Override
    public String getNestedUnitIdentityText(NestedUnitIdentity unitIdentity) {
      @NlsSafe String moduleName = unitIdentity.getModuleName();
      return moduleName;
    }

    @Override
    public Set<NestedUnitIdentity> createNestedModuleIdentitiesSet() {
      return new TreeSet<>();
    }
  }

  private class PlanBehavior extends Behavior {

    @Override
    public DMCompositeType getType() {
      return DMCompositeType.PLAN;
    }

    @Override
    public JRadioButton getRadioButton() {
      return myPlanRadio;
    }

    @Override
    public void updateEnablement() {
      myScopedCheckBox.setEnabled(true);
      myAtomicCheckBox.setEnabled(true);
      super.updateEnablement();
      myVersionsButton.setEnabled(myNestedBundlesList.getSelectedIndex() >= 0);
      boolean hasExactlyOneSelectedNestedUnit = myNestedBundlesList.getSelectedIndices().length == 1;
      myMoveUpButton.setEnabled(hasExactlyOneSelectedNestedUnit && myNestedBundlesList.getSelectedIndex() > 0);
      myMoveDownButton
        .setEnabled(hasExactlyOneSelectedNestedUnit && myNestedBundlesList.getSelectedIndex() < myNestedBundlesListModel.size() - 1);
    }

    @Override
    public boolean transformAtomic(boolean atomic) {
      return atomic;
    }

    @Override
    public boolean transformScoped(boolean scoped) {
      return scoped;
    }

    @Override
    @Nls
    protected String getChooseModulesDialogTitle() {
      return DmServerBundle.message("DMModuleCompositeFacetSettingsPanel.PlanBehavior.chooser.title");
    }

    @Override
    @Nls
    public String getNestedUnitIdentityText(NestedUnitIdentity unitIdentity) {
      @NlsSafe String moduleName = unitIdentity.getModuleName();
      @NlsSafe String versionRange = unitIdentity.getVersionRange();
      return MessageFormat.format("{0} {1}", moduleName, versionRange);
    }

    @Override
    public Set<NestedUnitIdentity> createNestedModuleIdentitiesSet() {
      return new LinkedHashSet<>();
    }
  }
}
