package com.intellij.vaadin.actions;

import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.facet.GwtSdkPathEditor;
import com.intellij.gwt.packaging.GwtCompilerOutputElement;
import com.intellij.gwt.sdk.GwtSdk;
import com.intellij.javaee.artifact.JavaeeArtifactUtil;
import com.intellij.javaee.web.artifact.WebArtifactUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.vaadin.VaadinBundle;
import com.intellij.vaadin.framework.VaadinConstants;
import com.intellij.vaadin.framework.VaadinVersion;
import com.intellij.vaadin.framework.VaadinVersionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;

public class CreateWidgetDialog extends DialogWrapper {
  @NotNull private final Module myModule;
  @NotNull private final InputValidator myValidator;
  private JPanel myMainPanel;
  private JTextField myNameField;
  private JPanel myGwtSdkPathPanel;
  private JLabel mySetupGwtSdkLabel;
  private final GwtSdkPathEditor mySdkPathEditor;

  public CreateWidgetDialog(@NotNull Module module, @NotNull InputValidator validator) {
    super(module.getProject());
    setTitle(VaadinBundle.message("dialog.title.create.new.widget"));
    myModule = module;
    myValidator = validator;
    GwtFacet gwtFacet = GwtFacet.getInstance(module);
    if (gwtFacet == null) {
      mySdkPathEditor = new GwtSdkPathEditor(module.getProject());
      VaadinVersion version = VaadinVersionUtil.getVaadinVersion(module);
      if (version.isFullDistributionRequired()) {
        String path = VaadinVersionUtil.detectVaadinHome(module);
        if (path != null) {
          mySdkPathEditor.setPath(path);
        }
      }
      myGwtSdkPathPanel.add(mySdkPathEditor.getMainComponent());
    }
    else {
      mySdkPathEditor = null;
      myGwtSdkPathPanel.setVisible(false);
      mySetupGwtSdkLabel.setVisible(false);
    }
    init();
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return myNameField;
  }

  @Nullable
  @Override
  protected JComponent createNorthPanel() {
    return myMainPanel;
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return null;
  }

  @Override
  protected void doOKAction() {
    final String name = myNameField.getText().trim();
    if (myValidator.checkInput(name)) {
      if (mySdkPathEditor != null) {
        final GwtSdk sdk = mySdkPathEditor.getSelectedSdk();
        WriteAction.runAndWait(() -> {
          GwtFacet gwtFacet = GwtFacet.createNewFacet(myModule, sdk);
          WebFacet webFacet = ContainerUtil.getFirstItem(WebFacet.getInstances(myModule));
          if (webFacet != null) {
            gwtFacet.getConfiguration().setWebFacetName(webFacet.getName());
            Project project = myModule.getProject();
            ArtifactManager artifactManager = ArtifactManager.getInstance(project);
            PackagingElementResolvingContext context = artifactManager.getResolvingContext();
            Collection<? extends ArtifactType> types = WebArtifactUtil.getInstance().getWebArtifactTypes();
            for (Artifact artifact : JavaeeArtifactUtil.getInstance().getArtifactsContainingFacet(webFacet, context, types, false)) {
              artifactManager
                .addElementsToDirectory(artifact, VaadinConstants.VAADIN_WIDGET_SETS_PATH, new GwtCompilerOutputElement(project, gwtFacet));
            }
          }
        });
      }
      if (myValidator.canClose(name)) {
        close(OK_EXIT_CODE);
      }
    }
  }
}
