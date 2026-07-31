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

package com.intellij.gwt.facet;

import com.intellij.facet.FacetManager;
import com.intellij.facet.impl.ui.FacetEditorContextBase;
import com.intellij.facet.ui.FacetConfigurationQuickFix;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetEditorValidator;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.make.GwtCompilerWorkspaceConfiguration;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.psi.GwtLanguageLevelPusher;
import com.intellij.gwt.sdk.GwtSdk;
import com.intellij.gwt.sdk.GwtSdkManager;
import com.intellij.gwt.sdk.GwtSdkType;
import com.intellij.gwt.sdk.GwtSdkUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.TableUtil;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.dsl.listCellRenderer.BuilderKt;
import com.intellij.ui.dsl.listCellRenderer.LcrJavaHelper;
import com.intellij.ui.dsl.listCellRenderer.RendererPresentation;
import com.intellij.ui.table.TableView;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.gwt.model.GwtJavaScriptOutputStyle;
import org.jetbrains.jps.gwt.model.impl.sdk.GwtSdkPathUtil;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class GwtFacetEditor extends FacetEditorTab {
  private final JComboBox<GwtJavaScriptOutputStyle> myOutputStyleBox;
  private final JPanel myMainPanel;
  private final JComboBox<String> myWebFacetBox;
  private final JTextField myCompilerHeapSizeField;
  private final JPanel myGwtSdkEditorPlace;
  private final JPanel myPackagingPathsPanel;
  private final JTextField myCompilerVMParametersField;
  private final JTextField myCompilerParametersField;
  private final JPanel myGwtSdkPathPanel;
  private final JLabel myUsedSdkLabel;
  private final JCheckBox myShowCompilerOutputCheckBox;
  private final JPanel myPackagingTablePanel;
  private final FacetEditorContext myEditorContext;
  private final GwtSdkPathEditor myGwtPathEditor;
  private final FacetValidatorsManager myValidatorsManager;
  private final GwtFacetConfiguration myConfiguration;
  private final TableView<ModulePackagingInfo> myTableView;
  private List<ModulePackagingInfo> myOriginalModulePackagingInfos;
  private final List<ModulePackagingInfo> myModulePackagingInfos;
  private final ListTableModel<ModulePackagingInfo> myTableModel;

  public GwtFacetEditor(final FacetEditorContext editorContext,
                        final FacetValidatorsManager validatorsManager,
                        GwtFacetConfiguration configuration) {
    myEditorContext = editorContext;
    myValidatorsManager = validatorsManager;
    myConfiguration = configuration;
    {
      // GUI initializer generated by IntelliJ IDEA GUI Designer
      // >>> IMPORTANT!! <<<
      // DO NOT EDIT OR ADD ANY CODE HERE!
      myMainPanel = new JPanel();
      myMainPanel.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
      final JPanel panel1 = new JPanel();
      panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
      myMainPanel.add(panel1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                  GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                  GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null,
                                                  null, 0, false));
      final JPanel panel2 = new JPanel();
      panel2.setLayout(new GridLayoutManager(6, 2, new Insets(0, 0, 0, 0), -1, -1));
      panel2.putClientProperty("BorderFactoryClass", "com.intellij.ui.IdeBorderFactory$PlainSmallWithIndent");
      panel1.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                             GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                             GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                                             0, false));
      panel2.setBorder(IdeBorderFactory.PlainSmallWithIndent.createTitledBorder(BorderFactory.createLineBorder(Color.black),
                                                                                this.$$$getMessageFromBundle$$$("messages/GwtBundle",
                                                                                                                "border.title.gwt.compilation"),
                                                                                TitledBorder.DEFAULT_JUSTIFICATION,
                                                                                TitledBorder.DEFAULT_POSITION, null, null));
      final JLabel label1 = new JLabel();
      this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("messages/GwtBundle", "label.select.script.output.style.text"));
      panel2.add(label1,
                 new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                                     GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      myOutputStyleBox = new JComboBox();
      panel2.add(myOutputStyleBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                                                       GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
                                                       new Dimension(100, -1), null, 0, false));
      final JLabel label2 = new JLabel();
      this.$$$loadLabelText$$$(label2, this.$$$getMessageFromBundle$$$("messages/GwtBundle", "label.text.gwt.compiler.heap.size"));
      panel2.add(label2,
                 new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                                     GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      myCompilerHeapSizeField = new JTextField();
      panel2.add(myCompilerHeapSizeField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                                                              GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
                                                              new Dimension(25, -1), null, 0, false));
      myPackagingPathsPanel = new JPanel();
      myPackagingPathsPanel.setLayout(new CardLayout(0, 0));
      panel2.add(myPackagingPathsPanel, new GridConstraints(5, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                                                            new Dimension(-1, 50), null, null, 0, false));
      myPackagingTablePanel = new JPanel();
      myPackagingTablePanel.setLayout(new BorderLayout(0, 0));
      myPackagingPathsPanel.add(myPackagingTablePanel, "table");
      final JPanel panel3 = new JPanel();
      panel3.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
      myPackagingPathsPanel.add(panel3, "notAvailable");
      final JBLabel jBLabel1 = new JBLabel();
      this.$$$loadLabelText$$$(jBLabel1, this.$$$getMessageFromBundle$$$("messages/GwtBundle",
                                                                         "label.gwt.modules.are.not.available.while.index.update.is.in.progress"));
      panel3.add(jBLabel1,
                 new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                                     GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      final Spacer spacer1 = new Spacer();
      panel3.add(spacer1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
                                              GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
      final JLabel label3 = new JLabel();
      this.$$$loadLabelText$$$(label3, this.$$$getMessageFromBundle$$$("messages/GwtBundle", "label.text.additional.compiler.parameters"));
      panel2.add(label3,
                 new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                                     GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      myCompilerVMParametersField = new JTextField();
      panel2.add(myCompilerVMParametersField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                                                                  GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED,
                                                                  null, new Dimension(150, -1), null, 0, false));
      final JLabel label4 = new JLabel();
      this.$$$loadLabelText$$$(label4, this.$$$getMessageFromBundle$$$("messages/GwtBundle", "label.text.gwt.compiler.parameters"));
      panel2.add(label4,
                 new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                                     GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      myCompilerParametersField = new JTextField();
      panel2.add(myCompilerParametersField, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                                                                GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED,
                                                                null, new Dimension(150, -1), null, 0, false));
      myShowCompilerOutputCheckBox = new JCheckBox();
      this.$$$loadButtonText$$$(myShowCompilerOutputCheckBox,
                                this.$$$getMessageFromBundle$$$("messages/GwtBundle", "checkbox.show.compiler.output"));
      panel2.add(myShowCompilerOutputCheckBox, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                                   GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                                   GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
                                                                   null, null, null, 0, false));
      myGwtSdkPathPanel = new JPanel();
      myGwtSdkPathPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
      myMainPanel.add(myGwtSdkPathPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                             GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                             GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                             null, null, null, 0, false));
      myGwtSdkEditorPlace = new JPanel();
      myGwtSdkEditorPlace.setLayout(new BorderLayout(0, 0));
      myGwtSdkPathPanel.add(myGwtSdkEditorPlace, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                                     GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                                     GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                     GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                                     GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
      final JLabel label5 = new JLabel();
      this.$$$loadLabelText$$$(label5, this.$$$getMessageFromBundle$$$("messages/GwtBundle", "label.text.path.to.gwt.installation"));
      myGwtSdkPathPanel.add(label5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
                                                        null, 0, false));
      final JPanel panel4 = new JPanel();
      panel4.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
      myMainPanel.add(panel4, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                  GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                  GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null,
                                                  null, 0, false));
      final JLabel label6 = new JLabel();
      this.$$$loadLabelText$$$(label6, this.$$$getMessageFromBundle$$$("messages/GwtBundle", "label.select.web.facet.text"));
      panel4.add(label6,
                 new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                                     GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
      final Spacer spacer2 = new Spacer();
      panel4.add(spacer2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                              GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
      myWebFacetBox = new JComboBox();
      panel4.add(myWebFacetBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                                                    GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null,
                                                    0, false));
      myUsedSdkLabel = new JLabel();
      this.$$$loadLabelText$$$(myUsedSdkLabel, this.$$$getMessageFromBundle$$$("messages/GwtBundle", "label.label"));
      myMainPanel.add(myUsedSdkLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                          GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
                                                          null, 0, false));
      label1.setLabelFor(myOutputStyleBox);
      label2.setLabelFor(myCompilerHeapSizeField);
      label3.setLabelFor(myCompilerVMParametersField);
      label4.setLabelFor(myCompilerParametersField);
      label6.setLabelFor(myWebFacetBox);
    }

    Project project = editorContext.getProject();
    myGwtPathEditor = new GwtSdkPathEditor(project);
    myGwtSdkEditorPlace.add(myGwtPathEditor.getMainComponent(), BorderLayout.CENTER);

    final Module module = editorContext.getModule();
    myWebFacetBox.addItem(null);
    for (WebFacet webFacet : editorContext.getFacetsProvider().getFacetsByType(module, WebFacet.ID)) {
      myWebFacetBox.addItem(webFacet.getName());
    }
    myWebFacetBox.setSelectedIndex(0);

    setupGwtOutputStyleCombobox(myOutputStyleBox);
    myWebFacetBox.setRenderer(LcrJavaHelper.create(
      GwtBundle.message("label.none.facet.selected"),
      value -> {
        WebFacet webFacet = FacetManager.getInstance(module).findFacet(WebFacet.ID, value);
        return new RendererPresentation(webFacet != null ? webFacet.getType().getIcon() : null, value);
      }
    ));

    validatorsManager.registerValidator(new FacetEditorValidator() {
      @Override
      public @NotNull ValidationResult check() {
        String type = myConfiguration.getGwtSdkType();
        if (type != null) return ValidationResult.OK;

        String path = myGwtPathEditor.getPath();
        ValidationResult result = GwtSdkUtil.checkGwtSdkPath(path);
        if (result.isOk()) {
          result = checkUserJarLibrary(path);
        }
        return result;
      }
    }, myGwtPathEditor.getComboBox());

    validatorsManager.registerValidator(new GwtFacetArtifactValidator((GwtFacet)editorContext.getFacet(), myEditorContext, this));

    final ColumnInfo[] columns = {ENABLED_COLUMN, MODULE_NAME_COLUMN, OUTPUT_PATH_COLUMN};
    myModulePackagingInfos = new ArrayList<>();
    myOriginalModulePackagingInfos = new ArrayList<>();
    myTableModel = new ListTableModel<>(columns, myModulePackagingInfos, 0);
    myTableView = new TableView<>(myTableModel);
    TableColumnModel columnModel = myTableView.getColumnModel();
    TableColumn column = columnModel.getColumn(0);
    column.setCellRenderer(new BooleanTableCellRenderer() {
      @Override
      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSel, boolean hasFocus, int row, int column) {
        Component component = super.getTableCellRendererComponent(table, value, isSel, hasFocus, row, column);
        String tooltip = getTooltip(row);
        setToolTipText(tooltip);
        return component;
      }
    });
    TableUtil.setupCheckboxColumn(column);
    columnModel.getColumn(1).setCellRenderer(new TableCellRendererWithTooltip());
    columnModel.getColumn(2).setCellRenderer(new TableCellRendererWithTooltip());
    myPackagingTablePanel.add(ScrollPaneFactory.createScrollPane(myTableView), BorderLayout.CENTER);
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

  private @Nullable @NlsContexts.Tooltip String getTooltip(int row) {
    List<ModulePackagingInfo> items = myTableModel.getItems();
    if (0 <= row && row < items.size()) {
      ModulePackagingInfo info = items.get(row);
      if (info.myLibraryModule) {
        return GwtBundle.message("tooltip.gwt.modules.without.entry.points.cannot.be.compiled");
      }
    }
    return null;
  }

  public static void setupGwtOutputStyleCombobox(final JComboBox<GwtJavaScriptOutputStyle> comboBox) {
    for (GwtJavaScriptOutputStyle style : GwtJavaScriptOutputStyle.values()) {
      comboBox.addItem(style);
    }

    comboBox.setRenderer(BuilderKt.textListCellRenderer("", GwtJavaScriptOutputStyle::getPresentableName));
  }

  public JComboBox getOutputStyleBox() {
    return myOutputStyleBox;
  }

  public JTextField getCompilerHeapSizeField() {
    return myCompilerHeapSizeField;
  }

  public GwtSdkPathEditor getGwtPathEditor() {
    return myGwtPathEditor;
  }

  @Override
  public void onTabEntering() {
    myValidatorsManager.validate();
  }

  private ValidationResult checkUserJarLibrary(final String sdkPath) {
    final String userJarPath = GwtSdkPathUtil.getUserJarPath(sdkPath);
    final VirtualFile userJar =
      JarFileSystem.getInstance().findFileByPath(FileUtil.toSystemIndependentName(userJarPath) + JarFileSystem.JAR_SEPARATOR);
    ModuleRootModel rootModel = myEditorContext.getRootModel();
    if (userJar == null) return ValidationResult.OK;

    GwtLibrarySearchingPolicy searchingPolicy = new GwtLibrarySearchingPolicy(myEditorContext, userJar);
    boolean found = searchingPolicy.containsLibrary(rootModel);
    if (found) {
      return ValidationResult.OK;
    }

    String errorMessage;
    final LibraryOrderEntry gwtLibrary = searchingPolicy.getGwtLibrary();
    if (gwtLibrary == null) {
      errorMessage = GwtBundle.message("error.message.gwt.user.jar.library.not.found.in.dependencies.of.module");
    }
    else {
      errorMessage = GwtBundle.message("error.message.gwt.user.jar.in.library.0.does.not.correspond.to.selected.gwt.installation",
                                       gwtLibrary.getPresentableName());
    }

    return new ValidationResult(errorMessage, new FacetConfigurationQuickFix() {
      @Override
      public void run(final JComponent place) {
        ModifiableRootModel modifiableRootModel = myEditorContext.getModifiableRootModel();
        if (gwtLibrary != null && gwtLibrary.isValid()) {
          modifiableRootModel.removeOrderEntry(gwtLibrary);
        }
        final LibrariesContainer container = ((FacetEditorContextBase)myEditorContext).getContainer();
        Library library = GwtSdkUtil.findOrCreateGwtUserLibrary(container, userJar);
        modifiableRootModel.addLibraryEntry(library);
      }
    });
  }

  private GwtJavaScriptOutputStyle getOutputStyle() {
    return (GwtJavaScriptOutputStyle)myOutputStyleBox.getSelectedItem();
  }

  private JPanel getMainPanel() {
    return myMainPanel;
  }

  @Override
  public @NotNull JComponent createComponent() {
    return getMainPanel();
  }

  @Override
  public boolean isModified() {
    return !myConfiguration.getGwtSdkPath().equals(myGwtPathEditor.getPath())
           ||
           myConfiguration.getOutputStyle() != getOutputStyle()
           ||
           !Objects.equals(myConfiguration.getWebFacetName(), getSelectedWebFacet())
           ||
           !myConfiguration.getAdditionalCompilerVMParameters().equals(myCompilerVMParametersField.getText())
           ||
           !myConfiguration.getCompilerParameters().equals(myCompilerParametersField.getText())
           ||
           !String.valueOf(myConfiguration.getCompilerMaxHeapSize()).equals(myCompilerHeapSizeField.getText())
           ||
           getWorkspaceConfiguration().isShowCompilerOutput((GwtFacet)myEditorContext.getFacet()) !=
           myShowCompilerOutputCheckBox.isSelected()
           ||
           isPackagingSettingsModified();
  }

  private boolean isPackagingSettingsModified() {
    return !Comparing.haveEqualElements(myOriginalModulePackagingInfos, myModulePackagingInfos) || myTableView.isEditing();
  }

  @Override
  public void apply() {
    String gwtUrl = myGwtPathEditor.getUrl();

    myConfiguration.setGwtSdkUrlAndType(gwtUrl);
    final GwtSdk gwtSdk = myConfiguration.getSdk();
    GwtSdkManager.getInstance().moveToTop(gwtSdk);
    myConfiguration.setOutputStyle(getOutputStyle());
    myConfiguration.setWebFacetName(getSelectedWebFacet());
    myConfiguration.setAdditionalCompilerVMParameters(myCompilerVMParametersField.getText().trim());
    setCompilerParameters();
    try {
      myConfiguration.setCompilerMaxHeapSize(Integer.parseInt(myCompilerHeapSizeField.getText().trim()));
    }
    catch (NumberFormatException e) {
      //todo show warning
    }
    for (ModulePackagingInfo packagingInfo : myModulePackagingInfos) {
      String outputPath = packagingInfo.myOutputPath;
      if (packagingInfo.myDefaultOutputPath.equals(outputPath)) {
        outputPath = null;
      }
      myConfiguration.setPackagingRelativePath(packagingInfo.myModuleName, outputPath);
      myConfiguration.setModuleCompilationEnabled(packagingInfo.myModuleName, packagingInfo.myEnabled);
    }
    getWorkspaceConfiguration().setShowCompilerOutput((GwtFacet)myEditorContext.getFacet(), myShowCompilerOutputCheckBox.isSelected());
  }

  private void setCompilerParameters() {
    LanguageLevel oldLevel = myConfiguration.getClientLanguageLevel();
    myConfiguration.setCompilerParameters(myCompilerParametersField.getText().trim());
    LanguageLevel newLevel = myConfiguration.getClientLanguageLevel();

    if (newLevel != oldLevel) {
      GwtLanguageLevelPusher.updateConfigurationAndPush(myEditorContext.getProject(), true);
    }
  }

  private GwtCompilerWorkspaceConfiguration getWorkspaceConfiguration() {
    return GwtCompilerWorkspaceConfiguration.getInstance(myEditorContext.getProject());
  }

  public @Nullable String getSelectedWebFacet() {
    return (String)myWebFacetBox.getSelectedItem();
  }

  @Override
  public void reset() {
    myGwtPathEditor.setPath(myConfiguration.getGwtSdkPath());
    myOutputStyleBox.setSelectedItem(myConfiguration.getOutputStyle());
    myWebFacetBox.setSelectedItem(myConfiguration.getWebFacetName());
    myCompilerVMParametersField.setText(myConfiguration.getAdditionalCompilerVMParameters());
    myCompilerParametersField.setText(myConfiguration.getCompilerParameters());
    myCompilerHeapSizeField.setText(String.valueOf(myConfiguration.getCompilerMaxHeapSize()));

    Project project = myEditorContext.getProject();
    boolean gwtModulesAvailable = !DumbService.getInstance(project).isDumb();
    ((CardLayout)myPackagingPathsPanel.getLayout()).show(myPackagingPathsPanel, gwtModulesAvailable ? "table" : "notAvailable");
    myModulePackagingInfos.clear();
    if (gwtModulesAvailable) {
      myOriginalModulePackagingInfos = computePackagingInfos();
      myModulePackagingInfos.addAll(computePackagingInfos());

      int gwtModulesCount = myModulePackagingInfos.size();
      int height = (myTableView.getRowHeight() + myTableView.getRowMargin()) * gwtModulesCount +
                   myTableView.getTableHeader().getPreferredSize().height +
                   2;
      height = Math.max(height, myPackagingTablePanel.getMinimumSize().height);
      height = Math.min(height, 300);
      myPackagingTablePanel.setPreferredSize(new Dimension(myPackagingTablePanel.getPreferredSize().width, height));
      myTableModel.setItems(myModulePackagingInfos);
      myTableView.revalidate();
      myTableView.repaint();
    }

    if (myEditorContext.isNewFacet() && myConfiguration.getGwtSdkPath().isEmpty()) {
      GwtSdk gwtSdk = GwtSdkManager.getInstance().suggestGwtSdk();
      if (gwtSdk != null) {
        myGwtPathEditor.setPath(VfsUtilCore.urlToPath(gwtSdk.getHomeDirectoryUrl()));
      }
    }

    final GwtSdkType sdkType = GwtSdkType.findType(myConfiguration.getGwtSdkType());
    myUsedSdkLabel.setVisible(sdkType != null && !sdkType.isEditable());
    if (sdkType != null) {
      myUsedSdkLabel.setText(GwtBundle.message("label.text.0.is.used", sdkType.getPresentableName(myGwtPathEditor.getPath())));
    }
    myGwtSdkPathPanel.setVisible(sdkType == null || sdkType.isEditable());
    myShowCompilerOutputCheckBox.setSelected(getWorkspaceConfiguration().isShowCompilerOutput((GwtFacet)myEditorContext.getFacet()));
  }

  private List<ModulePackagingInfo> computePackagingInfos() {
    List<ModulePackagingInfo> list = new ArrayList<>();
    GwtModulesManager modulesManager = GwtModulesManager.getInstance(myEditorContext.getProject());
    for (GwtModule gwtModule : modulesManager.getGwtModules(myEditorContext.getModule(), true)) {
      list.add(new ModulePackagingInfo(gwtModule, myConfiguration, modulesManager.isLibraryModule(gwtModule)));
    }
    return list;
  }

  @Override
  public @Nls String getDisplayName() {
    return GwtBundle.message("google.web.toolkit.title");
  }

  @Override
  public @Nullable @NonNls String getHelpTopic() {
    return "reference.settings.project.modules.gwt.facet";
  }

  private static final class ModulePackagingInfo {
    private final String myModuleName;
    private String myOutputPath;
    private final String myDefaultOutputPath;
    private boolean myEnabled;
    private final boolean myLibraryModule;

    private ModulePackagingInfo(GwtModule gwtModule, GwtFacetConfiguration configuration, final boolean libraryModule) {
      myModuleName = gwtModule.getQualifiedName();
      myOutputPath = configuration.getPackagingRelativePath(gwtModule);
      myDefaultOutputPath = GwtFacetConfiguration.getDefaultPackagingPath(gwtModule);
      myEnabled = configuration.isModuleCompilationEnabled(gwtModule) && !libraryModule;
      myLibraryModule = libraryModule;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final ModulePackagingInfo that = (ModulePackagingInfo)o;
      return myModuleName.equals(that.myModuleName) && myOutputPath.equals(that.myOutputPath) && myEnabled == that.myEnabled;
    }

    @Override
    public int hashCode() {
      return 239 * myModuleName.hashCode() + 31 * myOutputPath.hashCode() + (myEnabled ? 1 : 0);
    }
  }

  private static final ColumnInfo<ModulePackagingInfo, Boolean> ENABLED_COLUMN = new ColumnInfo<>("") {
    @Override
    public Class getColumnClass() {
      return Boolean.class;
    }

    @Override
    public Boolean valueOf(final ModulePackagingInfo modulePackagingInfo) {
      return modulePackagingInfo.myEnabled;
    }

    @Override
    public boolean isCellEditable(ModulePackagingInfo modulePackagingInfo) {
      return !modulePackagingInfo.myLibraryModule;
    }

    @Override
    public void setValue(ModulePackagingInfo modulePackagingInfo, Boolean value) {
      modulePackagingInfo.myEnabled = value;
    }
  };

  private static final ColumnInfo<ModulePackagingInfo, String> MODULE_NAME_COLUMN =
    new ColumnInfo<>(GwtBundle.message("table.column.name.gwt.module")) {
      @Override
      public String valueOf(final ModulePackagingInfo modulePackagingInfo) {
        return modulePackagingInfo.myModuleName;
      }
    };

  private static final ColumnInfo<ModulePackagingInfo, String> OUTPUT_PATH_COLUMN =
    new ColumnInfo<>(GwtBundle.message("table.column.name.output.relative.path")) {
      @Override
      public String valueOf(final ModulePackagingInfo modulePackagingInfo) {
        return modulePackagingInfo.myOutputPath;
      }

      @Override
      public boolean isCellEditable(final ModulePackagingInfo modulePackagingInfo) {
        return !modulePackagingInfo.myLibraryModule;
      }

      @Override
      public void setValue(final ModulePackagingInfo modulePackagingInfo, final String value) {
        modulePackagingInfo.myOutputPath = value;
      }
    };

  private class TableCellRendererWithTooltip extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      setToolTipText(getTooltip(row));
      return component;
    }
  }
}
