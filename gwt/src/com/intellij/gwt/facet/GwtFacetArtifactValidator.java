package com.intellij.gwt.facet;

import com.intellij.facet.impl.ui.FacetEditorContextBase;
import com.intellij.facet.ui.FacetConfigurationQuickFix;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorValidator;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.packaging.GwtCompileOutputRelativePathSuggester;
import com.intellij.gwt.packaging.GwtCompilerOutputElement;
import com.intellij.gwt.packaging.GwtCompilerOutputElementType;
import com.intellij.gwt.rpc.GwtServletUtil;
import com.intellij.gwt.sdk.GwtSdk;
import com.intellij.javaee.artifact.JavaeeArtifactUtil;
import com.intellij.javaee.web.artifact.WebArtifactUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.artifacts.ArtifactEditorEx;
import com.intellij.openapi.roots.ui.configuration.artifacts.ArtifactsStructureConfigurableContext;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.artifacts.ModifiableArtifact;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.elements.PackagingElementFactory;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.packaging.impl.artifacts.PackagingElementPath;
import com.intellij.packaging.impl.artifacts.PackagingElementProcessor;
import com.intellij.packaging.impl.artifacts.PlainArtifactType;
import com.intellij.packaging.impl.elements.FileCopyPackagingElement;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JComponent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GwtFacetArtifactValidator extends FacetEditorValidator {
  private final GwtFacet myFacet;
  private final FacetEditorContext myEditorContext;
  private final GwtFacetEditor myGwtFacetEditor;

  public GwtFacetArtifactValidator(GwtFacet facet, FacetEditorContext editorContext, GwtFacetEditor gwtFacetEditor) {
    myFacet = facet;
    myEditorContext = editorContext;
    myGwtFacetEditor = gwtFacetEditor;
  }

  @Override
  public @NotNull ValidationResult check() {
    final ArtifactsStructureConfigurableContext artifactsContext = ((FacetEditorContextBase)myEditorContext).getArtifactsStructureContext();
    for (Artifact artifact : artifactsContext.getArtifactModel().getArtifacts()) {
      if (!ArtifactUtil.processPackagingElements(artifact, GwtCompilerOutputElementType.getInstance(),
                                                 element -> !myFacet.equals(element.getFacet()), artifactsContext, false)) {
        final ArtifactType artifactType = artifact.getArtifactType();
        if (WebArtifactUtil.getInstance().isWebApplication(artifactType) && !containsServletJar(artifactsContext, artifact, artifactType)
            && GwtServletUtil.hasServlets(myFacet)) {
          return new ValidationResult(
            GwtBundle.message("validation.result.gwt.servlet.jar.is.not.included.in.0.artifact", artifact.getName()),
            new IncludeGwtServletJarQuickFix(artifact, artifactsContext));
        }
        return ValidationResult.OK;
      }
    }

    final List<? extends Artifact> artifacts = new ArrayList<Artifact>(getSuitableArtifacts(artifactsContext));
    artifacts.sort(ArtifactManager.ARTIFACT_COMPARATOR);

    final FacetConfigurationQuickFix fix;
    if (artifacts.isEmpty()) {
      fix = new FacetConfigurationQuickFix(GwtBundle.message("quickfix.text.create.artifact")) {
        @Override
        public void run(JComponent place) {
          createArtifact(artifactsContext);
        }
      };
    }
    else {
      fix = new FacetConfigurationQuickFix() {
        @Override
        public void run(JComponent place) {
          final String createItem = GwtBundle.message("quickfix.text.create.artifact");
          final String includeItem = artifacts.size() == 1 ? GwtBundle.message("quickfix.text.include.into.0", artifacts.get(0).getName())
                                                           : GwtBundle.message("quickfix.text.include.in.artifact");
          JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<>(null, createItem, includeItem) {
            @Override
            public boolean hasSubstep(String selectedValue) {
              return includeItem.equals(selectedValue) && artifacts.size() > 1;
            }

            @Override
            public PopupStep<?> onChosen(String selectedValue, boolean finalChoice) {
              if (selectedValue.equals(createItem)) {
                createArtifact(artifactsContext);
              }
              else if (artifacts.size() == 1) {
                includeInArtifact(artifacts.get(0), artifactsContext);
              }
              else {
                return new MySelectArtifactPopupStep(artifacts, artifactsContext);
              }
              return FINAL_CHOICE;
            }
          }).showUnderneathOf(place);
        }
      };
    }

    return new ValidationResult(GwtBundle.message("validation.result.gwt.compiler.output.is.not.included.in.an.artifact"), fix);
  }

  private boolean containsServletJar(ArtifactsStructureConfigurableContext artifactsContext, Artifact artifact, ArtifactType artifactType) {
    final GwtSdk sdk = myFacet.getConfiguration().getSdk();
    if (!new File(sdk.getServletJarPath()).exists()) {
      return true;
    }

    final String servletJarPath = FileUtil.toSystemIndependentName(sdk.getServletJarPath());
    return !ArtifactUtil.processDirectoryChildren(artifact.getRootElement(), PackagingElementPath.EMPTY, "/WEB-INF/lib", artifactsContext,
                                          artifactType, new PackagingElementProcessor<>() {
        @Override
        public boolean process(@NotNull PackagingElement<?> element, @NotNull PackagingElementPath path) {
          if (element instanceof FileCopyPackagingElement &&
              FileUtil.pathsEqual(FileUtil.toSystemIndependentName(((FileCopyPackagingElement)element).getFilePath()), servletJarPath)) {
            return false;
          }
          return true;
        }
      });
  }

  private void createArtifact(ArtifactsStructureConfigurableContext artifactsContext) {
    final String artifactName = suggestArtifactName(myFacet.getModule().getName());
    final ModifiableArtifact artifact = artifactsContext.getOrCreateModifiableArtifactModel().addArtifact(artifactName, PlainArtifactType.getInstance());
    includeInArtifact(artifact, artifactsContext);
  }

  public static @NonNls String suggestArtifactName(final String moduleName) {
    return moduleName + ":GWT";
  }

  private void includeInArtifact(final Artifact artifact, final ArtifactsStructureConfigurableContext artifactsContext) {
    artifactsContext.editLayout(artifact, () -> {
      GwtCompilerOutputElement element = new GwtCompilerOutputElement(artifactsContext.getProject(), myFacet);
      String path = GwtCompileOutputRelativePathSuggester.suggestRelativeOutputPath(myFacet, artifactsContext);
      artifactsContext.getRootElement(artifact).addOrFindChild(PackagingElementFactory.getInstance().createParentDirectories(path, element));
    });
    ((ArtifactEditorEx)artifactsContext.getOrCreateEditor(artifact)).rebuildTries();
    ProjectStructureConfigurable.getInstance(artifactsContext.getProject()).select(artifact, true);
  }

  private Collection<? extends Artifact> getSuitableArtifacts(ArtifactsStructureConfigurableContext artifactsContext) {
    final String webFacetName = myGwtFacetEditor.getSelectedWebFacet();
    if (webFacetName != null) {
      final WebFacet webFacet = artifactsContext.getModulesProvider().getFacetModel(myFacet.getModule()).findFacet(WebFacet.ID, webFacetName);
      if (webFacet != null) {
        final Collection<Artifact> artifacts = JavaeeArtifactUtil.getInstance().getArtifactsContainingFacet(webFacet, artifactsContext,
                                                                                                            WebArtifactUtil.getInstance().getWebArtifactTypes(), false);
        if (!artifacts.isEmpty()) {
          return artifacts;
        }
      }
    }
    return artifactsContext.getArtifactModel().getArtifactsByType(WebArtifactUtil.getInstance().getExplodedWarArtifactType());
  }

  private class MySelectArtifactPopupStep extends BaseListPopupStep<Artifact> {
    private final ArtifactsStructureConfigurableContext myArtifactsContext;

    MySelectArtifactPopupStep(List<? extends Artifact> artifacts, ArtifactsStructureConfigurableContext artifactsContext) {
      super(GwtBundle.message("popup.step.title.select.artifact"), artifacts);
      myArtifactsContext = artifactsContext;
    }

    @Override
    public @NotNull String getTextFor(Artifact value) {
      return value.getName();
    }

    @Override
    public Icon getIconFor(Artifact aValue) {
      return aValue.getArtifactType().getIcon();
    }

    @Override
    public PopupStep<?> onChosen(Artifact selectedValue, boolean finalChoice) {
      includeInArtifact(selectedValue, myArtifactsContext);
      return FINAL_CHOICE;
    }
  }

  private class IncludeGwtServletJarQuickFix extends FacetConfigurationQuickFix {
    private final Artifact myArtifact;
    private final ArtifactsStructureConfigurableContext myArtifactsContext;

    IncludeGwtServletJarQuickFix(Artifact artifact, ArtifactsStructureConfigurableContext artifactsContext) {
      myArtifact = artifact;
      myArtifactsContext = artifactsContext;
    }

    @Override
    public void run(JComponent place) {
      myArtifactsContext.editLayout(myArtifact, () -> {
        final CompositePackagingElement<?> root = myArtifactsContext.getRootElement(myArtifact);
        PackagingElementFactory.getInstance().addFileCopy(root, "WEB-INF/lib", FileUtil.toSystemIndependentName(myFacet.getConfiguration().getSdk().getServletJarPath()));
      });
      ((ArtifactEditorEx)myArtifactsContext.getOrCreateEditor(myArtifact)).rebuildTries();
    }
  }
}
