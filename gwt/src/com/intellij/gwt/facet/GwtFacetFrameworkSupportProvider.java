/*
 * Copyright 2000-2007 JetBrains s.r.o.
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

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.facet.FacetManager;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.facet.ProjectFacetManager;
import com.intellij.facet.ui.FacetBasedFrameworkSupportProvider;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.icons.GwtIcons;
import com.intellij.gwt.packaging.GwtCompileOutputRelativePathSuggester;
import com.intellij.gwt.packaging.GwtCompilerOutputElement;
import com.intellij.gwt.run.GwtRunConfiguration;
import com.intellij.gwt.run.GwtRunConfigurationType;
import com.intellij.gwt.sdk.GwtSdk;
import com.intellij.gwt.sdk.GwtSdkManager;
import com.intellij.gwt.sdk.GwtSdkUtil;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.frameworkSupport.FrameworkRole;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportConfigurable;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportProvider;
import com.intellij.ide.util.newProjectWizard.impl.FrameworkSupportModelBase;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.javaee.artifact.JavaeeArtifactUtil;
import com.intellij.javaee.framework.JavaeeProjectCategory;
import com.intellij.javaee.web.artifact.WebArtifactUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.ui.configuration.FacetsProvider;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainerFactory;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.elements.PackagingElementFactory;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.ResourceBundle;

public final class GwtFacetFrameworkSupportProvider extends FrameworkSupportProvider {
  public GwtFacetFrameworkSupportProvider() {
    super(FacetBasedFrameworkSupportProvider.getProviderId(GwtFacetType.ID), GwtBundle.message("framework.title.google.web.toolkit"));
  }

  @Override
  public String[] getPrecedingFrameworkProviderIds() {
    return new String[]{FacetBasedFrameworkSupportProvider.getProviderId(WebFacet.ID)};
  }

  @Override
  public @NotNull FrameworkSupportConfigurable createConfigurable(final @NotNull FrameworkSupportModel model) {
    return new GwtFrameworkSupportConfigurable(model);
  }

  @Override
  public Icon getIcon() {
    return GwtIcons.GoogleSmall;
  }

  @Override
  public boolean isSupportAlreadyAdded(@NotNull Module module, @NotNull FacetsProvider facetsProvider) {
    return !facetsProvider.getFacetsByType(module, GwtFacetType.ID).isEmpty();
  }

  @Override
  public boolean isEnabledForModuleType(@NotNull ModuleType moduleType) {
    return moduleType instanceof JavaModuleType;
  }

  @Override
  public boolean isEnabledForModuleBuilder(@NotNull ModuleBuilder builder) {
    return false;
  }

  @Override
  public FrameworkRole[] getRoles() {
    return new FrameworkRole[]{JavaeeProjectCategory.ROLE};
  }

  public static final class GwtFrameworkSupportConfigurable extends FrameworkSupportConfigurable {
    private final GwtSdkPathEditor mySdkPathEditor;
    private final JComponent myMainPanel;
    private final JPanel mySdkEditorPlace;
    private final JCheckBox myCreateSampleAppCheckBox;
    private final JTextField myAppNameField;
    private final HyperlinkLabel myErrorLabel;
    private final FrameworkSupportModel myModel;

    private GwtFrameworkSupportConfigurable(FrameworkSupportModel model) {
      myModel = model;
      {
        // GUI initializer generated by IntelliJ IDEA GUI Designer
        // >>> IMPORTANT!! <<<
        // DO NOT EDIT OR ADD ANY CODE HERE!
        myMainPanel = new JPanel();
        myMainPanel.setLayout(new GridLayoutManager(6, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("messages/GwtBundle", "label.gwt.sdk"));
        myMainPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                    GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                                                    false));
        final Spacer spacer1 = new Spacer();
        myMainPanel.add(spacer1, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
                                                     GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        mySdkEditorPlace = new JPanel();
        mySdkEditorPlace.setLayout(new BorderLayout(0, 0));
        myMainPanel.add(mySdkEditorPlace, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                              GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                              GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                              null, null, null, 0, false));
        myCreateSampleAppCheckBox = new JCheckBox();
        this.$$$loadButtonText$$$(myCreateSampleAppCheckBox,
                                  this.$$$getMessageFromBundle$$$("messages/GwtBundle", "checkbox.create.sample.application"));
        myMainPanel.add(myCreateSampleAppCheckBox, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                                       GridConstraints.SIZEPOLICY_CAN_SHRINK |
                                                                       GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                       GridConstraints.SIZEPOLICY_FIXED,
                                                                       null, null, null, 0, false));
        myAppNameField = new JTextField();
        myAppNameField.setEnabled(true);
        myAppNameField.setText("com.MySampleApplication"); //NON-NLS
        myMainPanel.add(myAppNameField, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                                                            GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
                                                            new Dimension(150, -1), null, 1, false));
        myErrorLabel = new HyperlinkLabel();
        myMainPanel.add(myErrorLabel, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                                          GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                          GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null,
                                                          null, null, 0, false));
      }
      mySdkPathEditor = new GwtSdkPathEditor(null);
      myErrorLabel.setIcon(AllIcons.General.BalloonError);
      myErrorLabel.setHyperlinkTarget(GwtSdkUtil.GWT_DOWNLOAD_URL);
      Project defaultProject = ProjectManager.getInstance().getDefaultProject();
      String path = ProjectFacetManager.getInstance(defaultProject).createDefaultConfiguration(GwtFacetType.getInstance()).getGwtSdkPath();
      if (StringUtil.isEmpty(path)) {
        GwtSdk sdk = GwtSdkManager.getInstance().suggestGwtSdk();
        if (sdk != null) {
          path = VfsUtilCore.urlToPath(sdk.getHomeDirectoryUrl());
        }
      }
      mySdkPathEditor.setPath(path);
      mySdkEditorPlace.add(BorderLayout.CENTER, mySdkPathEditor.getMainComponent());

      myCreateSampleAppCheckBox.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          myAppNameField.setEnabled(myCreateSampleAppCheckBox.isSelected());
        }
      });
      mySdkPathEditor.getPathTextField().getDocument().addDocumentListener(new DocumentAdapter() {
        @Override
        protected void textChanged(@NotNull DocumentEvent e) {
          checkSdk();
        }
      });
      checkSdk();
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

    public GwtSdkPathEditor getSdkPathEditor() {
      return mySdkPathEditor;
    }

    private void checkSdk() {
      final String path = mySdkPathEditor.getPath();
      if (StringUtil.isEmptyOrSpaces(path)) {
        myErrorLabel.setVisible(true);
        myErrorLabel.setHyperlinkText(GwtBundle.message("label.text.gwt.sdk.path.not.specified"),
                                      GwtBundle.message("label.text.download.gwt"), "");
      }
      else {
        myErrorLabel.setVisible(false);
      }
      myMainPanel.repaint();
    }

    @Override
    public void onFrameworkSelectionChanged(boolean selected) {
      myAppNameField.setEnabled(selected && myCreateSampleAppCheckBox.isSelected());
    }

    @Override
    public JComponent getComponent() {
      return myMainPanel;
    }

    @Override
    public void addSupport(final @NotNull Module module, final @NotNull ModifiableRootModel rootModel, final @Nullable Library library) {
      FacetManager facetManager = FacetManager.getInstance(module);
      ModifiableFacetModel facetModel = facetManager.createModifiableModel();
      GwtFacet facet = facetManager.createFacet(GwtFacetType.getInstance(), GwtFacetType.getInstance().getDefaultFacetName(), null);
      Collection<WebFacet> facets = WebFacet.getInstances(facet.getModule());
      final WebFacet webFacet = ContainerUtil.getFirstItem(facets, null);

      final Project project = module.getProject();
      final ArtifactManager artifactManager = ArtifactManager.getInstance(project);
      Collection<Artifact> artifacts = Collections.emptyList();
      if (webFacet != null) {
        facet.getConfiguration().setWebFacetName(webFacet.getName());

        final PackagingElementResolvingContext context = artifactManager.getResolvingContext();
        artifacts = JavaeeArtifactUtil.getInstance()
          .getArtifactsContainingFacet(webFacet, context, WebArtifactUtil.getInstance().getWebArtifactTypes(), false);
        for (Artifact artifact : artifacts) {
          String path = GwtCompileOutputRelativePathSuggester.suggestRelativeOutputPath(myModel);
          artifactManager.addElementsToDirectory(artifact, path, new GwtCompilerOutputElement(project, facet));
        }
      }

      facetModel.addFacet(facet);
      facetModel.commit();
      String sdkUrl = VfsUtilCore.pathToUrl(mySdkPathEditor.getPath());
      GwtSdk gwtSdk = GwtSdkManager.getInstance().getGwtSdk(sdkUrl);
      GwtSdkManager.getInstance().moveToTop(gwtSdk);
      LibrariesContainer container = ((FrameworkSupportModelBase)myModel).getLibrariesContainer();
      if (!container.canCreateLibrary(LibrariesContainer.LibraryLevel.PROJECT)) {
        container = LibrariesContainerFactory.createContainer(module.getProject());
      }
      GwtFacet.setupGwtSdkAndLibraries(facet.getConfiguration(), rootModel, gwtSdk, container);

      if (myCreateSampleAppCheckBox.isSelected()) {
        GwtSampleApplicationCreator creator = new GwtSampleApplicationCreator(facet, myAppNameField.getText(), rootModel, myModel);

        for (Artifact artifact : artifacts) {
          addServletJarToArtifact(artifactManager, artifact, gwtSdk);
        }

        final RunManager runManager = RunManager.getInstance(module.getProject());
        final RunnerAndConfigurationSettings runSettings =
          runManager.createConfiguration(creator.getAppName(), GwtRunConfigurationType.getFactory());
        GwtRunConfiguration configuration = (GwtRunConfiguration)runSettings.getConfiguration();
        configuration.setModule(module);
        configuration.setPage(creator.getAppName() + ".html");
        if (gwtSdk.getVersion().isSuperDevModeUsedByDefault()) {
          configuration.getGwtState().USE_SUPER_DEV_MODE = true;
        }
        runManager.addConfiguration(runSettings);
        runManager.setSelectedConfiguration(runSettings);

        creator.create();
      }
    }
  }

  public static void addServletJarToArtifact(ArtifactManager artifactManager, Artifact artifact, GwtSdk gwtSdk) {
    final PackagingElement<?> element = PackagingElementFactory
      .getInstance().createFileCopyWithParentDirectories(FileUtil.toSystemIndependentName(gwtSdk.getServletJarPath()), "/");
    artifactManager.addElementsToDirectory(artifact, "/WEB-INF/lib", element);
  }
}
