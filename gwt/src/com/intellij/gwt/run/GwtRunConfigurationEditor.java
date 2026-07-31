/*
 * Copyright 2000-2006 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.gwt.run;

import com.intellij.application.options.ModulesComboBox;
import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.execution.configurations.RuntimeConfigurationWarning;
import com.intellij.execution.impl.ConfigurationSettingsEditorWrapper;
import com.intellij.execution.ui.CommonJavaParametersPanel;
import com.intellij.execution.ui.DefaultJreSelector;
import com.intellij.execution.ui.JrePathEditor;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.gwt.web.GwtWebUtil;
import com.intellij.ide.DataManager;
import com.intellij.ide.browsers.BrowserSelector;
import com.intellij.ide.browsers.JavaScriptDebuggerStarter;
import com.intellij.ide.browsers.WebBrowserManager;
import com.intellij.ide.highlighter.HtmlFileType;
import com.intellij.javaee.constants.JavaeeCommonConstants;
import com.intellij.javaee.ui.packaging.ExplodedWarArtifactType;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.DumbModeBlockedFunctionality;
import com.intellij.openapi.project.DumbModeBlockedFunctionalityCollector;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.messages.MessagesService;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactPointer;
import com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTask;
import com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTaskProvider;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.AnchorableComponent;
import com.intellij.ui.ComboboxWithBrowseButton;
import com.intellij.ui.PanelWithAnchor;
import com.intellij.ui.SortedComboBoxModel;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.dsl.listCellRenderer.BuilderKt;
import com.intellij.ui.dsl.listCellRenderer.LcrJavaHelper;
import com.intellij.ui.dsl.listCellRenderer.RendererPresentation;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ObjectUtils;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.descriptors.ConfigFile;
import com.intellij.util.execution.ParametersListUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractButton;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public final class GwtRunConfigurationEditor extends SettingsEditor<GwtRunConfiguration> implements PanelWithAnchor {
  private static final Comparator<List<String>> MODULES_LIST_COMPARATOR = (o1, o2) -> {
    if (o1.size() > o2.size()) return -1;
    if (o1.size() < o2.size()) return 1;
    return ContainerUtil.compareLexicographically(o1, o2, String.CASE_INSENSITIVE_ORDER);
  };
  private SortedComboBoxModel<List<String>> myGwtModulesModel;
  private DefaultComboBoxModel myPagesModel;
  private final CommonJavaParametersPanel myCommonProgramParameters;
  private final ModulesComboBox myModulesBox;
  private final JPanel myMainPanel;
  private final ComboboxWithBrowseButton myHtmlPageBox;
  private final JCheckBox myPatchWebXmlCheckBox;
  private final TextFieldWithBrowseButton myWebXmlField;
  private final ComboboxWithBrowseButton myGwtModuleComboBox;
  private final JBLabel myGwtModuleLabel;
  private final JBLabel myServerLabel;
  private final JComboBox<GwtDevModeServer> myServerComboBox;
  private final JBCheckBox myOpenInBrowserCheckBox;
  private final BrowserSelector myBrowserSelector;
  private final JPanel myBrowserSelectorPanel;
  private final JCheckBox myUpdateResourcesOnFrameCheckBox;
  private final JBCheckBox myUseSuperDevModeCheckBox;
  private final JBLabel myStartPageLabel;
  private final JCheckBox myStartJavaScriptDebuggerCheckBox;
  private final JBLabel myModuleLabel;
  private final JrePathEditor myJrePathEditor;
  private final Project myProject;
  private final GwtModulesManager myGwtModulesManager;
  private final Map<String, String> myHtmlPage2GwtModule = new HashMap<>();
  private List<String> myAllGwtModules = new ArrayList<>();
  private JComponent myAnchor;

  public GwtRunConfigurationEditor(final Project project) {
    myProject = project;
    {
      myCommonProgramParameters = new GwtCommonParametersPanel();
      myGwtModulesModel = new SortedComboBoxModel<>(MODULES_LIST_COMPARATOR);
      myGwtModuleComboBox = new ComboboxWithBrowseButton(new ComboBox<>(myGwtModulesModel));
    }
    {
      // GUI initializer generated by IntelliJ IDEA GUI Designer
      // >>> IMPORTANT!! <<<
      // DO NOT EDIT OR ADD ANY CODE HERE!
      myMainPanel = new JPanel();
      myMainPanel.setLayout(new GridLayoutManager(13, 2, new Insets(0, 0, 0, 0), -1, -1));
      myModuleLabel = new JBLabel();
      this.$$$loadLabelText$$$(myModuleLabel, this.$$$getMessageFromBundle$$$("messages/GwtBundle", "label.choose.module.text"));
      myMainPanel.add(myModuleLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                         GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
                                                         null, 0, false));
      final Spacer spacer1 = new Spacer();
      myMainPanel.add(spacer1, new GridConstraints(12, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
                                                   GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
      myModulesBox = new ModulesComboBox();
      myMainPanel.add(myModulesBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                                                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                                                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      myPatchWebXmlCheckBox = new JCheckBox();
      this.$$$loadButtonText$$$(myPatchWebXmlCheckBox,
                                this.$$$getMessageFromBundle$$$("messages/GwtBundle", "checkbox.text.use.custom.web.xml"));
      myMainPanel.add(myPatchWebXmlCheckBox, new GridConstraints(10, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                                 GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                                 GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
                                                                 null, null, null, 0, false));
      myWebXmlField = new TextFieldWithBrowseButton();
      myMainPanel.add(myWebXmlField, new GridConstraints(11, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                                         GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                         GridConstraints.SIZEPOLICY_FIXED, null, null, null, 2, false));
      myGwtModuleLabel = new JBLabel();
      this.$$$loadLabelText$$$(myGwtModuleLabel, this.$$$getMessageFromBundle$$$("messages/GwtBundle", "label.text.gwt.module.to.load"));
      myMainPanel.add(myGwtModuleLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                            GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
                                                            null, 0, false));
      myMainPanel.add(myGwtModuleComboBox, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                                                               GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                                                               GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      myOpenInBrowserCheckBox = new JBCheckBox();
      this.$$$loadButtonText$$$(myOpenInBrowserCheckBox, this.$$$getMessageFromBundle$$$("messages/GwtBundle", "checkbox.open.in.browser"));
      myMainPanel.add(myOpenInBrowserCheckBox, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                                   GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                                   GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
                                                                   null, null, null, 0, false));
      myBrowserSelectorPanel = new JPanel();
      myBrowserSelectorPanel.setLayout(new BorderLayout(0, 0));
      myMainPanel.add(myBrowserSelectorPanel, new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                                  GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                                  GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                  GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                                  GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
      myUpdateResourcesOnFrameCheckBox = new JCheckBox();
      this.$$$loadButtonText$$$(myUpdateResourcesOnFrameCheckBox,
                                this.$$$getMessageFromBundle$$$("messages/GwtBundle", "checkbox.update.resources.on.frame.deactivation"));
      myMainPanel.add(myUpdateResourcesOnFrameCheckBox,
                      new GridConstraints(9, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                          GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                          GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      myUseSuperDevModeCheckBox = new JBCheckBox();
      this.$$$loadButtonText$$$(myUseSuperDevModeCheckBox,
                                this.$$$getMessageFromBundle$$$("messages/GwtBundle", "checkbox.use.super.dev.mode"));
      myMainPanel.add(myUseSuperDevModeCheckBox, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                                     GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED,
                                                                     null, null, null, 0, false));
      myStartJavaScriptDebuggerCheckBox = new JCheckBox();
      this.$$$loadButtonText$$$(myStartJavaScriptDebuggerCheckBox,
                                this.$$$getMessageFromBundle$$$("messages/GwtBundle", "checkbox.with.javascript.debugger"));
      myMainPanel.add(myStartJavaScriptDebuggerCheckBox,
                      new GridConstraints(8, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                          GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                          GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
      myMainPanel.add(myCommonProgramParameters,
                      new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                          GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                                          false));
      myServerComboBox = new JComboBox();
      myMainPanel.add(myServerComboBox, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                                                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                                                            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      myServerLabel = new JBLabel();
      this.$$$loadLabelText$$$(myServerLabel, this.$$$getMessageFromBundle$$$("messages/GwtBundle", "label.server"));
      myMainPanel.add(myServerLabel, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                         GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
                                                         null, 0, false));
      myStartPageLabel = new JBLabel();
      this.$$$loadLabelText$$$(myStartPageLabel, this.$$$getMessageFromBundle$$$("messages/GwtBundle", "label.html.to.open.text"));
      myMainPanel.add(myStartPageLabel, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                            GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
                                                            null, 0, false));
      myHtmlPageBox = new ComboboxWithBrowseButton();
      myMainPanel.add(myHtmlPageBox, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                                         GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                                                         GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null,
                                                         null, null, 0, false));
      myJrePathEditor = new JrePathEditor();
      myMainPanel.add(myJrePathEditor, new GridConstraints(6, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                                           GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
                                                           null, null, 0, false));
      myModuleLabel.setLabelFor(myModulesBox);
      myGwtModuleLabel.setLabelFor(myGwtModuleComboBox);
      myServerLabel.setLabelFor(myServerComboBox);
      myStartPageLabel.setLabelFor(myHtmlPageBox);
    }
    myGwtModulesManager = GwtModulesManager.getInstance(myProject);
    myBrowserSelector = new BrowserSelector();
    myGwtModuleComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (DumbService.isDumb(project)) {
          DumbModeBlockedFunctionalityCollector.INSTANCE.logFunctionalityBlocked(project, DumbModeBlockedFunctionality.Gwt);
          Messages.showErrorDialog(myMainPanel,
                                   GwtBundle.message("message.text.gwt.module.chooser.isn.t.available.while.updating.indices"));
          return;
        }
        ChooseGwtModulesDialog dialog = new ChooseGwtModulesDialog(getSelectedModule(), getSelectedGwtModules());
        if (dialog.showAndGet()) {
          setSelectedGwtModules(dialog.getSelectedModules());
        }
      }
    });
    myJrePathEditor.setDefaultJreSelector(DefaultJreSelector.fromModuleDependencies(myModulesBox, false));
    myAnchor = UIUtil.mergeComponentsWithAnchor(myCommonProgramParameters, myJrePathEditor,
                                                convert(myModuleLabel), convert(myGwtModuleLabel),
                                                convert(myServerLabel), convert(myStartPageLabel),
                                                convert(myOpenInBrowserCheckBox));
  }

  private static Method $$$cachedGetBundleMethod$$$ = null;

  /** @noinspection ALL */
  private String $$$getMessageFromBundle$$$(String path, String key) {
    ResourceBundle bundle;
    try {
      Class<?> thisClass = this.getClass();
      if ($$$cachedGetBundleMethod$$$ == null) {
        Class<?> dynamicBundleClass = thisClass.getClassLoader().loadClass("com.intellij.DynamicBundle");
        $$$cachedGetBundleMethod$$$ = dynamicBundleClass.getMethod("getBundle", String.class, Class.class);
      }
      bundle = (ResourceBundle)$$$cachedGetBundleMethod$$$.invoke(null, path, thisClass);
    }
    catch (Exception e) {
      bundle = ResourceBundle.getBundle(path);
    }
    return bundle.getString(key);
  }

  /** @noinspection ALL */
  private void $$$loadLabelText$$$(JLabel component, String text) {
    StringBuffer result = new StringBuffer();
    boolean haveMnemonic = false;
    char mnemonic = '\0';
    int mnemonicIndex = -1;
    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) == '&') {
        i++;
        if (i == text.length()) break;
        if (!haveMnemonic && text.charAt(i) != '&') {
          haveMnemonic = true;
          mnemonic = text.charAt(i);
          mnemonicIndex = result.length();
        }
      }
      result.append(text.charAt(i));
    }
    component.setText(result.toString());
    if (haveMnemonic) {
      component.setDisplayedMnemonic(mnemonic);
      component.setDisplayedMnemonicIndex(mnemonicIndex);
    }
  }

  /** @noinspection ALL */
  private void $$$loadButtonText$$$(AbstractButton component, String text) {
    StringBuffer result = new StringBuffer();
    boolean haveMnemonic = false;
    char mnemonic = '\0';
    int mnemonicIndex = -1;
    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) == '&') {
        i++;
        if (i == text.length()) break;
        if (!haveMnemonic && text.charAt(i) != '&') {
          haveMnemonic = true;
          mnemonic = text.charAt(i);
          mnemonicIndex = result.length();
        }
      }
      result.append(text.charAt(i));
    }
    component.setText(result.toString());
    if (haveMnemonic) {
      component.setMnemonic(mnemonic);
      component.setDisplayedMnemonicIndex(mnemonicIndex);
    }
  }

  /** @noinspection ALL */
  public JComponent $$$getRootComponent$$$() { return myMainPanel; }

  @Override
  public void resetEditorFrom(@NotNull GwtRunConfiguration configuration) {
    myCommonProgramParameters.reset(configuration);
    final GwtRunConfiguration.GwtRunConfigurationState state = configuration.getGwtState();
    myCommonProgramParameters.setVMParameters(state.VM_PARAMETERS);
    myUpdateResourcesOnFrameCheckBox.setSelected(state.UPDATE_RESOURCES_ON_FRAME_DEACTIVATION);
    myUseSuperDevModeCheckBox.setSelected(state.USE_SUPER_DEV_MODE);

    Module module = configuration.getModule();
    myModulesBox.setModules(configuration.getValidModules());
    myModulesBox.setSelectedModule(module);

    boolean customWebXml = state.CUSTOM_WEB_XML != null;
    myPatchWebXmlCheckBox.setSelected(customWebXml);
    myWebXmlField.setEnabled(customWebXml);
    if (customWebXml) {
      setCustomWebXml(state.CUSTOM_WEB_XML);
    }
    updateWebXmlPanel(module);

    fillPages(module);
    String pagePath = configuration.getPage();
    if (pagePath == null) {
      pagePath = "";
    }
    myHtmlPageBox.getComboBox().getEditor().setItem(pagePath);

    updateGwtModulesCombobox(module);
    myGwtModuleComboBox.getComboBox().setSelectedItem(null);
    final List<String> gwtModules = state.getGwtModules();
    setSelectedGwtModules(gwtModules != null ? gwtModules : myAllGwtModules);

    for (int i = 0; i < myServerComboBox.getItemCount(); i++) {
      GwtDevModeServer server = myServerComboBox.getItemAt(i);
      if (server.getId().equals(state.SERVER_ID)) {
        myServerComboBox.setSelectedIndex(i);
        break;
      }
    }

    myOpenInBrowserCheckBox.setSelected(state.OPEN_IN_BROWSER);
    myStartJavaScriptDebuggerCheckBox.setSelected(state.START_JAVASCRIPT_DEBUGGER);
    myBrowserSelector.setSelected(WebBrowserManager.getInstance().findBrowserById(state.BROWSER));
    updateSuperDevModeCheckbox(module);
    updateOpenInBrowserSection();
    updateUpdateResourcesCheckbox();
    updateServerCombobox();
    updateStartPagePanel();
    myJrePathEditor.setPathOrName(state.ALTERNATIVE_JRE_PATH, true);
  }

  private void setSelectedGwtModules(@NotNull List<String> gwtModules) {
    if (myGwtModulesModel.indexOf(gwtModules) == -1) {
      for (List<String> list : new ArrayList<>(myGwtModulesModel.getItems())) {
        if (list.size() > 1 && !list.equals(myAllGwtModules)) {
          myGwtModulesModel.remove(list);
        }
      }
      myGwtModulesModel.add(gwtModules);
    }
    myGwtModuleComboBox.getComboBox().setSelectedItem(gwtModules);
  }

  private void updateStartPagePanel() {
    myStartPageLabel.setVisible(true);
    myHtmlPageBox.setVisible(true);
    myStartJavaScriptDebuggerCheckBox.setVisible(myUseSuperDevModeCheckBox.isSelected() && JavaScriptDebuggerStarter.Util.hasStarters());
  }

  private void updateGwtModulesCombobox(@Nullable Module module) {
    myAllGwtModules = new ArrayList<>();
    myGwtModulesModel.clear();
    boolean enable = false;
    if (module != null) {
      for (GwtModule gwtModule : myGwtModulesManager.getCompilableGwtModules(module, false)) {
        String moduleName = gwtModule.getQualifiedName();
        myAllGwtModules.add(moduleName);
        myGwtModulesModel.add(Collections.singletonList(moduleName));
      }
      final GwtFacet facet = GwtFacet.getInstance(module);
      if (facet != null) {
        enable = facet.getSdkVersion().isModulesToLoadSpecifiedInDevMode();
      }
    }
    myAllGwtModules.sort(String.CASE_INSENSITIVE_ORDER);
    setSelectedGwtModules(myAllGwtModules);
    myGwtModuleComboBox.setEnabled(enable);
    myGwtModuleLabel.setEnabled(enable);
  }

  private void setCustomWebXml(final String url) {
    myWebXmlField.setText(FileUtil.toSystemDependentName(VfsUtilCore.urlToPath(url)));
  }

  private @Nullable VirtualFile getFileByPagePath(final @Nullable Module module, final @NotNull String pagePath) {
    final int index = pagePath.indexOf('/');
    if (index == -1) return null;

    final GlobalSearchScope scope =
      module != null ? GlobalSearchScope.moduleWithDependenciesScope(module) : GlobalSearchScope.allScope(myProject);
    Collection<GwtModule> gwtModules = myGwtModulesManager.findGwtModulesByOutputName(pagePath.substring(0, index), scope);
    String name = pagePath.substring(index + 1);
    for (GwtModule gwtModule : gwtModules) {
      final Collection<VirtualFile> publicRoots = gwtModule.getPublicRoots();
      for (VirtualFile root : publicRoots) {
        final VirtualFile file = root.findFileByRelativePath(name);
        if (file != null) {
          return file;
        }
      }
    }
    return null;
  }

  private void fillPages(final Module module) {
    myPagesModel.removeAllElements();
    myHtmlPage2GwtModule.clear();
    if (module == null) return;

    for (GwtModule gwtModule : myGwtModulesManager.getCompilableGwtModules(module, false)) {
      final Collection<VirtualFile> htmlFiles = myGwtModulesManager.getAllHtmlFiles(gwtModule);
      for (VirtualFile htmlFile : htmlFiles) {
        String path = myGwtModulesManager.getOutputPath(gwtModule, htmlFile);
        if (path != null) {
          myHtmlPage2GwtModule.put(path, gwtModule.getQualifiedName());
          myPagesModel.addElement(path);
        }
      }
    }
  }

  @Override
  public void applyEditorTo(@NotNull GwtRunConfiguration configuration) throws ConfigurationException {
    configuration.setModule(getSelectedModule());
    final String path = (String)myHtmlPageBox.getComboBox().getEditor().getItem();
    configuration.setPage(path);

    validateDevModeParameters();
    myCommonProgramParameters.applyTo(configuration);
    final GwtRunConfiguration.GwtRunConfigurationState state = configuration.getGwtState();
    state.VM_PARAMETERS = myCommonProgramParameters.getVMParameters();
    state.UPDATE_RESOURCES_ON_FRAME_DEACTIVATION = myUpdateResourcesOnFrameCheckBox.isSelected();
    state.USE_SUPER_DEV_MODE = isUseSuperDevModeSelected();
    if (myPatchWebXmlCheckBox.isSelected()) {
      state.CUSTOM_WEB_XML = VfsUtilCore.pathToUrl(myWebXmlField.getText());
    }
    else {
      state.CUSTOM_WEB_XML = null;
    }
    List<String> selectedGwtModules = getSelectedGwtModules();
    state.setGwtModules(selectedGwtModules.equals(myAllGwtModules) ? null : selectedGwtModules);
    state.SERVER_ID = ((GwtDevModeServer)myServerComboBox.getSelectedItem()).getId();
    state.OPEN_IN_BROWSER = myOpenInBrowserCheckBox.isSelected();
    state.START_JAVASCRIPT_DEBUGGER = myStartJavaScriptDebuggerCheckBox.isSelected();
    state.BROWSER = myBrowserSelector.getSelectedBrowserId();
    state.ALTERNATIVE_JRE_PATH = myJrePathEditor.getJrePathOrName();
  }

  private void validateDevModeParameters() throws ConfigurationException {
    String devModeParameters = myCommonProgramParameters.getProgramParametersComponent().getComponent().getText();
    @NonNls List<String> devModeParametersList = ParametersListUtil.parse(devModeParameters);
    @NlsSafe String noServerArg = "-noserver";
    boolean noServer = devModeParametersList.contains(noServerArg);
    @NlsSafe String noStartServerArg = "-nostartServer";
    boolean noStartServer = devModeParametersList.contains(noStartServerArg);
    if (noServer || noStartServer) {
      int warIndex = 1 + devModeParametersList.indexOf("-war");
      if (warIndex == 0 || warIndex == devModeParametersList.size() || '-' == devModeParametersList.get(warIndex).charAt(0)) {
        String message =
          GwtBundle.message("error.message.0.parameter.used.without.corresponding.war", noServer ? noServerArg : noStartServerArg);

        DataContext dataContext = DataManager.getInstance().getDataContext(myMainPanel);
        ConfigurationSettingsEditorWrapper editorWrapper = ConfigurationSettingsEditorWrapper.CONFIGURATION_EDITOR_KEY.getData(dataContext);
        if (editorWrapper != null) {
          List<Artifact> artifacts = new SmartList<>();
          for (BeforeRunTask<?> beforeRunTask : editorWrapper.getStepsBeforeLaunch()) {
            if (beforeRunTask.getProviderId() == BuildArtifactsBeforeRunTaskProvider.ID) {
              BuildArtifactsBeforeRunTask task = (BuildArtifactsBeforeRunTask)beforeRunTask;
              for (ArtifactPointer pointer : task.getArtifactPointers()) {
                Artifact artifact = pointer.getArtifact();
                if (artifact != null && artifact.getArtifactType() instanceof ExplodedWarArtifactType) {
                  artifacts.add(artifact);
                }
              }
            }
          }
          if (artifacts.size() == 1) {
            Artifact artifact = artifacts.get(0);
            throw new RuntimeConfigurationWarning(message, () -> {
              if (warIndex == 0) {
                devModeParametersList.add("-war");
                devModeParametersList.add(artifact.getOutputPath());
              }
              else {
                devModeParametersList.add(warIndex, artifact.getOutputPath());
              }
              myCommonProgramParameters.getProgramParametersComponent().getComponent().setText(ParametersList.join(devModeParametersList));
            });
          }
        }
        throw new RuntimeConfigurationWarning(message);
      }
    }
  }

  private @NotNull List<String> getSelectedGwtModules() {
    return ObjectUtils.notNull(myGwtModulesModel.getSelectedItem(), myAllGwtModules);
  }

  private Module getSelectedModule() {
    return myModulesBox.getSelectedModule();
  }

  @Override
  public @NotNull JComponent createEditor() {
    myPagesModel = new DefaultComboBoxModel();
    final JComboBox comboBox = myHtmlPageBox.getComboBox();
    comboBox.setEditable(true);
    comboBox.setModel(myPagesModel);
    comboBox.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          String page = (String)e.getItem();
          final String gwtModule = myHtmlPage2GwtModule.get(page);
          if (gwtModule != null) {
            ensureGwtModuleIsSelected(gwtModule);
          }
        }
      }
    });

    myServerComboBox.setRenderer(LcrJavaHelper.create(
      "",
      value -> new RendererPresentation(value.getIcon(), value.getName())
    ));
    for (GwtDevModeServerProvider serverProvider : GwtDevModeServerProvider.EP_NAME.getExtensions()) {
      for (GwtDevModeServer server : serverProvider.getServers()) {
        myServerComboBox.addItem(server);
      }
    }

    myPatchWebXmlCheckBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        myWebXmlField.setEnabled(myPatchWebXmlCheckBox.isSelected());
      }
    });
    myWebXmlField.addBrowseFolderListener(myProject, createWebXmlChooserDescriptor());

    myModulesBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Module module = myModulesBox.getSelectedModule();
        updateSuperDevModeCheckbox(module);
        fillPages(module);
        updateWebXmlPanel(module);
        updateGwtModulesCombobox(module);
        updateOpenInBrowserSection();
        updateUpdateResourcesCheckbox();
      }
    });

    myUseSuperDevModeCheckBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        updateSuperDevModeCheckbox(getSelectedModule());
        updateServerCombobox();
        updateWebXmlPanel(getSelectedModule());
        updateUpdateResourcesCheckbox();
        updateStartPagePanel();
      }
    });
    myBrowserSelectorPanel.add(BorderLayout.CENTER, myBrowserSelector.getMainComponent());
    myOpenInBrowserCheckBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        updateOpenInBrowserSection();
      }
    });
    myHtmlPageBox.addActionListener(new HtmlPageActionListener());

    myGwtModuleComboBox.getComboBox().setRenderer(BuilderKt.<List<String>>textListCellRenderer("", value ->
    {
      if (myAllGwtModules.equals(value)) {
        return GwtBundle.message("text.gwt.modules.to.load.all");
      }
      else if (value.size() > 1) {
        return GwtBundle.message("item.text.0.and.more.modules", value.get(0), value.size() - 1, value.size() == 2 ? 0 : 1);
      }
      else if (value.size() == 1) {
        return value.get(0);
      }
      else {
        return GwtBundle.message("text.gwt.modules.to.load.none");
      }
    }));

    return myMainPanel;
  }

  private void ensureGwtModuleIsSelected(@NotNull String gwtModule) {
    List<String> modules = getSelectedGwtModules();
    if (!modules.contains(gwtModule)) {
      myGwtModuleComboBox.getComboBox().setSelectedItem(Collections.singletonList(gwtModule));
    }
  }

  private void updateServerCombobox() {
    boolean visible = myServerComboBox.getItemCount() >= 2;
    myServerComboBox.setVisible(visible);
    myServerLabel.setVisible(visible);
  }

  private boolean isUseSuperDevModeSelected() {
    return myUseSuperDevModeCheckBox.isVisible() && myUseSuperDevModeCheckBox.isSelected();
  }

  private void updateSuperDevModeCheckbox(@Nullable Module module) {
    GwtVersion version = GwtFacet.getGwtVersion(module);
    myUseSuperDevModeCheckBox.setVisible(version.isSuperDevModeSupported());
  }

  private void updateUpdateResourcesCheckbox() {
    myUpdateResourcesOnFrameCheckBox.setVisible(GwtFacet.getGwtVersion(getSelectedModule()).isOutOfProcessHostedModeSupported());
  }

  private void updateOpenInBrowserSection() {
    final boolean visible = GwtFacet.getGwtVersion(getSelectedModule()).isOutOfProcessHostedModeSupported();
    myOpenInBrowserCheckBox.setVisible(visible);
    myBrowserSelector.getMainComponent().setVisible(visible);
    final boolean enable = myOpenInBrowserCheckBox.isSelected();
    myBrowserSelector.getMainComponent().setEnabled(enable);
    myStartJavaScriptDebuggerCheckBox.setEnabled(enable);
  }

  private FileChooserDescriptor createWebXmlChooserDescriptor() {
    var descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("xml")
      .withFileFilter(file -> file.getName().equals(JavaeeCommonConstants.WEB_XML));
    setContentRoots(descriptor);
    return descriptor;
  }

  private void updateWebXmlPanel(final @Nullable Module module) {
    boolean visible = updateWebXmlField(module);
    myWebXmlField.setVisible(visible);
    myPatchWebXmlCheckBox.setVisible(visible);
  }

  private boolean updateWebXmlField(final @Nullable Module module) {
    if (module == null) return false;
    Collection<WebFacet> webFacets = WebFacet.getInstances(module);
    GwtFacet facet = GwtFacet.getInstance(module);
    if (webFacets.isEmpty() || facet == null || facet.getSdkVersion().isHostedModeRequiresWebXml()) return false;

    if (myWebXmlField.getText().trim().isEmpty()) {
      WebFacet webFacet = facet.getWebFacet();
      if (webFacet == null) {
        webFacet = webFacets.iterator().next();
      }
      ConfigFile descriptor = webFacet.getWebXmlDescriptor();
      if (descriptor != null) {
        setCustomWebXml(descriptor.getUrl());
      }
    }
    return true;
  }

  private FileChooserDescriptor createHtmlFileChooserDescriptor() {
    var descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor(HtmlFileType.INSTANCE);
    setContentRoots(descriptor);
    return descriptor;
  }

  private void setContentRoots(FileChooserDescriptor descriptor) {
    final ProjectRootManager rootManager = ProjectRootManager.getInstance(myProject);
    descriptor.setRoots(rootManager.getContentRoots());
  }

  private class RunPageComponentAccessor implements TextComponentAccessor<JComboBox> {
    @Override
    public String getText(final JComboBox component) {
      String pagePath = component.getEditor().getItem().toString();
      VirtualFile file = getFileByPagePath(getSelectedModule(), pagePath);
      return file != null ? file.getPath() : "";
    }

    @Override
    public void setText(final JComboBox component, final @NotNull String text) {
      throw new UnsupportedOperationException();
    }
  }

  private class HtmlPageActionListener extends ComponentWithBrowseButton.BrowseFolderActionListener<JComboBox> {
    HtmlPageActionListener() {
      super(myHtmlPageBox, myProject, createHtmlFileChooserDescriptor(), new RunPageComponentAccessor());
    }

    @Override
    protected void onFileChosen(final @NotNull VirtualFile chosenFile) {
      List<Pair<GwtModule, String>> pairs = myGwtModulesManager.findGwtModulesByPublicFile(chosenFile);
      Pair<GwtModule, String> pair = null;
      if (pairs.size() == 1) {
        pair = pairs.get(0);
      }
      else if (!pairs.isEmpty()) {
        String[] gwtModules = new String[pairs.size()];
        for (int i = 0; i < pairs.size(); i++) {
          gwtModules[i] = pairs.get(i).getFirst().getQualifiedName();
        }
        int answer =
          MessagesService.getInstance().showChooseDialog(myProject, myMainPanel, GwtBundle.message("choose.text.select.gwt.module"),
                                                         GwtBundle.message("dialog.title.choose.gwt.module"), gwtModules,
                                                         gwtModules[0], null);
        if (answer >= 0) {
          pair = pairs.get(answer);
        }
      }
      if (pair != null) {
        myHtmlPageBox.getComboBox().getEditor().setItem(GwtWebUtil.getOutputPath(pair.getFirst(), pair.getSecond()));
        ensureGwtModuleIsSelected(pair.getFirst().getQualifiedName());
      }
      else {
        final String path = GwtWebUtil.getRelativeToWebRootPath(chosenFile, myProject);
        if (path != null) {
          myHtmlPageBox.getComboBox().getEditor().setItem(path);
        }
        else {
          Messages.showErrorDialog(myMainPanel,
                                   GwtBundle.message("error.message.0.is.not.under.public.roots.of.gwt.modules.nor.under.web.roots",
                                                     chosenFile.getName()));
        }
      }
    }
  }

  @Override
  public void setAnchor(JComponent anchor) {
    myAnchor = anchor;

    myCommonProgramParameters.setAnchor(anchor);
    myJrePathEditor.setAnchor(anchor);
    myModuleLabel.setAnchor(anchor);
    myGwtModuleLabel.setAnchor(anchor);
    myServerLabel.setAnchor(anchor);
    myStartPageLabel.setAnchor(anchor);
    myOpenInBrowserCheckBox.setAnchor(anchor);
  }

  @Override
  public JComponent getAnchor() {
    return myAnchor;
  }

  // WORKAROUND
  // We have two identical interfaces that do not inherit one another.
  // This method converts instance of one interface to the instance of another interface
  // to reuse already implemented functionality.
  private static PanelWithAnchor convert(AnchorableComponent component) {
    return new PanelWithAnchor() {
      @Override
      public JComponent getAnchor() {
        JComponent anchor = component.getAnchor();
        return anchor == null ? (JComponent)component : anchor;
      }

      @Override
      public void setAnchor(@Nullable JComponent anchor) {
        component.setAnchor(anchor);
      }
    };
  }
}
