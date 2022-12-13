package com.intellij.dmserver.artifacts;

import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.ui.ArtifactEditorContext;
import icons.DmServerSupportIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DMCompositePackagingElementType extends WithModulePackagingElementType<DMCompositePackagingElement> {
  @NonNls
  public static final String TYPE_ID = "packaging-for-dm-composite";

  public static DMCompositePackagingElementType getInstance() {
    return getInstance(DMCompositePackagingElementType.class);
  }

  public DMCompositePackagingElementType() {
    super(TYPE_ID, DmServerBundle.messagePointer("DMCompositePackagingElementType.presentable.name"));
  }

  @Override
  public boolean canCreate(@NotNull ArtifactEditorContext context, @NotNull Artifact artifact) {
    return artifact.getArtifactType() instanceof DMCompositeArtifactTypeBase;
  }

  @Override
  public Icon getCreateElementIcon() {
    return DmServerSupportIcons.DM;
  }

  @NotNull
  @Override
  public DMCompositePackagingElement createEmpty(@NotNull Project project) {
    return new DMCompositePackagingElement(this, project, null);
  }

  @NotNull
  @Override
  public List<? extends PackagingElement<?>> chooseAndCreate(
    @NotNull ArtifactEditorContext context, @NotNull Artifact artifact, @NotNull CompositePackagingElement<?> parent) {
    List<Module> modules = chooseModules(context);
    final List<DMCompositePackagingElement> result = new ArrayList<>();
    for (Module next : modules) {
      result.add(createFor(next));
    }
    return result;
  }

  public static List<Module> chooseModules(ArtifactEditorContext context) {
    return context.chooseModules(Arrays.asList(context.getModulesProvider().getModules()),
                                 DmServerBundle.message("DMCompositePackagingElementType.chooser.title"));
  }
}
