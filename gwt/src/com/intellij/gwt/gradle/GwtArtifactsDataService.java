package com.intellij.gwt.gradle;

import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.packaging.GwtCompilerOutputElement;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.project.PackagingModifiableModel;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ModifiableArtifact;
import com.intellij.packaging.artifacts.ModifiableArtifactModel;
import com.intellij.packaging.elements.PackagingElementFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.data.War;
import org.jetbrains.plugins.gradle.model.data.WebConfigurationModelData;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.util.Collection;

import static com.intellij.gwt.packaging.GwtCompileOutputRelativePathSuggester.suggestRelativeOutputPath;

// Adds GWT compiler tweaks to WARs, Web artifacts is handled by WebModuleGradleDataService
@Order(ExternalSystemConstants.UNORDERED)
public final class GwtArtifactsDataService extends AbstractProjectDataService<WebConfigurationModelData, Artifact> {

  @Override
  public @NotNull Key<WebConfigurationModelData> getTargetDataKey() {
    return WebConfigurationModelData.KEY;
  }

  @Override
  public void importData(@NotNull Collection<? extends DataNode<WebConfigurationModelData>> toImport,
                         @Nullable ProjectData projectData,
                         @NotNull Project project,
                         @NotNull IdeModifiableModelsProvider modelsProvider) {
    for (DataNode<WebConfigurationModelData> node : toImport) {
      WebConfigurationModelData data = node.getData();
      if (GradleConstants.SYSTEM_ID.equals(data.getOwner())) {
        DataNode<?> parentNode = node.getParent();
        if (parentNode == null) continue;

        Object parentNodeData = parentNode.getData();
        if (parentNodeData instanceof ModuleData moduleData) {
          configureGwtModule(moduleData, data, modelsProvider);
        }
      }
    }
  }

  private String resolveArtifactName(@NotNull ModuleData moduleData, @NotNull String archiveName) {
    String artifactNamePrefix = GradleConstants.SYSTEM_ID.getReadableName();
    String artifactName;
    StringBuilder buf = new StringBuilder(artifactNamePrefix);
    if (!StringUtil.isEmpty(moduleData.getGroup())) {
      buf.append(" : ").append(moduleData.getGroup());
    }
    artifactName = adjustName(buf.toString(), archiveName);
    return artifactName + " (exploded)";
  }

  private String adjustName(@NotNull String namePrefix, @Nullable String archiveName) {
    String buffer = namePrefix + (StringUtil.isEmpty(archiveName) ? "" : " : " + archiveName);
    return buffer.replace('/', '_');
  }

  private void configureGwtModule(@NotNull ModuleData moduleData,
                                  @NotNull WebConfigurationModelData javaeeData,
                                  @NotNull IdeModifiableModelsProvider modelsProvider) {
    Module module = modelsProvider.findIdeModule(moduleData);
    if (module != null) {
      GwtFacet gwtFacet = GwtFacet.getInstance(module);
      if (gwtFacet != null) {
        Project project = module.getProject();
        PackagingModifiableModel packagingModifiableModel = modelsProvider.getModifiableModel(PackagingModifiableModel.class);
        String relativePath = suggestRelativeOutputPath(gwtFacet, packagingModifiableModel.getPackagingElementResolvingContext());

        ModifiableArtifactModel modifiableArtifactModel = packagingModifiableModel.getModifiableArtifactModel();
        for (War war : javaeeData.getArtifacts()) {
          String explodedArtifactName = resolveArtifactName(moduleData, war.getName());
          addGwtCompilerOutput(modifiableArtifactModel, project, gwtFacet, explodedArtifactName, relativePath);
        }
      }
    }
  }

  private static void addGwtCompilerOutput(@NotNull ModifiableArtifactModel modifiableArtifactModel, @NotNull Project project,
                                           @NotNull GwtFacet gwtFacet, @NotNull String artifactName, @NotNull String relativePath) {
    Artifact artifact = modifiableArtifactModel.findArtifact(artifactName);
    if (artifact != null) {
      ModifiableArtifact modifiableArtifact = modifiableArtifactModel.getOrCreateModifiableArtifact(artifact);
      GwtCompilerOutputElement element = new GwtCompilerOutputElement(project, gwtFacet);
      PackagingElementFactory.getInstance().getOrCreateDirectory(modifiableArtifact.getRootElement(), relativePath).addOrFindChild(element);
    }
  }
}
