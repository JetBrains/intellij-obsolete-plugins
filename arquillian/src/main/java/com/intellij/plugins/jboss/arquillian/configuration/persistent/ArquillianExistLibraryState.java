package com.intellij.plugins.jboss.arquillian.configuration.persistent;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.StructureConfigurableContext;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import org.jetbrains.annotations.NotNull;

@Tag("exist-library")
public class ArquillianExistLibraryState extends ArquillianLibraryState {
  @NlsSafe
  @NotNull
  @Attribute("name")
  public String name;

  @NotNull
  @Attribute("level")
  public String level;

  @SuppressWarnings("unused")
  private ArquillianExistLibraryState() {
    name = "";
    level = "";
  }

  public ArquillianExistLibraryState(@NotNull String name, @NotNull String level) {
    this.name = name;
    this.level = level;
  }

  public Library findLibrary(Project project) {
    final StructureConfigurableContext context = ProjectStructureConfigurable.getInstance(project).getContext();
    context.resetLibraries();
    return context.getLibrary(name, level);
  }

  @Override
  public <R> R accept(Visitor<R> visitor) {
    return visitor.visitExistLibrary(this);
  }
}
