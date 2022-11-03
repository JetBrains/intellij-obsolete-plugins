package com.intellij.dmserver.artifacts;

import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.ui.ArtifactEditorContext;
import icons.DmServerSupportIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.dmserver.model.JpsDMContainerPackagingElement;
import org.osmorc.facet.OsmorcFacet;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class DMContainerPackagingElementType extends WithModulePackagingElementType<DMContainerPackagingElement> {

  public static DMContainerPackagingElementType getInstance() {
    return getInstance(DMContainerPackagingElementType.class);
  }

  public DMContainerPackagingElementType() {
    super(JpsDMContainerPackagingElement.TYPE_ID, DmServerBundle.messagePointer("DMContainerPackagingElementType.presentable.name"));
  }

  @Override
  public boolean canCreate(@NotNull ArtifactEditorContext context, @NotNull Artifact artifact) {
    return artifact.getArtifactType() instanceof DMBundleArtifactType;
  }

  @Override
  public Icon getCreateElementIcon() {
    return DmServerSupportIcons.Bundle;
  }

  @NotNull
  @Override
  public DMContainerPackagingElement createEmpty(@NotNull Project project) {
    return new DMContainerPackagingElement(this, project, null);
  }

  @NotNull
  @Override
  public List<? extends PackagingElement<?>> chooseAndCreate(
    @NotNull ArtifactEditorContext context, @NotNull Artifact artifact, @NotNull CompositePackagingElement<?> parent) {
    List<Module> modules = chooseModules(context);
    final List<DMContainerPackagingElement> result = new ArrayList<>();
    for (Module next : modules) {
      if (OsmorcFacet.hasOsmorcFacet(next)) {
        result.add(createFor(next));
      }
    }
    return result;
  }

  public static List<Module> chooseModules(ArtifactEditorContext context) {
    List<Module> osmorcModules = new ArrayList<>();
    for (Module nextModule : context.getModulesProvider().getModules()) {
      if (OsmorcFacet.hasOsmorcFacet(nextModule)) {
        osmorcModules.add(nextModule);
      }
    }
    return context.chooseModules(osmorcModules, DmServerBundle.message("DMContainerPackagingElementType.chooser.title"));
  }
}
