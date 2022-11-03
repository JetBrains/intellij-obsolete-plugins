package com.intellij.dmserver.artifacts;

import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DMConfigPackagingElementType extends WithModulePackagingElementType<DMConfigPackagingElement> {
  @NonNls
  public static final String TYPE_ID = "packaging-for-dm-config";

  public static DMConfigPackagingElementType getInstance() {
    return getInstance(DMConfigPackagingElementType.class);
  }

  public DMConfigPackagingElementType() {
    super(TYPE_ID, DmServerBundle.messagePointer("DMConfigPackagingElementType.presentable.name"));
  }

  @Override
  public boolean canCreate(@NotNull ArtifactEditorContext context, @NotNull Artifact artifact) {
    return artifact.getArtifactType() instanceof DMConfigArtifactType;
  }

  @Override
  public Icon getCreateElementIcon() {
    return PlatformIcons.PROPERTIES_ICON;
  }

  @NotNull
  @Override
  public DMConfigPackagingElement createEmpty(@NotNull Project project) {
    return new DMConfigPackagingElement(this, project, null);
  }

  @NotNull
  @Override
  public List<? extends PackagingElement<?>> chooseAndCreate(
    @NotNull ArtifactEditorContext context, @NotNull Artifact artifact, @NotNull CompositePackagingElement<?> parent) {
    List<Module> modules = chooseModules(context);
    final List<DMConfigPackagingElement> result = new ArrayList<>();
    for (Module next : modules) {
      result.add(createFor(next));
    }
    return result;
  }

  public static List<Module> chooseModules(ArtifactEditorContext context) {
    return context.chooseModules(Arrays.asList(context.getModulesProvider().getModules()),
                                 DmServerBundle.message("DMConfigPackagingElementType.chooser.label"));
  }
}
