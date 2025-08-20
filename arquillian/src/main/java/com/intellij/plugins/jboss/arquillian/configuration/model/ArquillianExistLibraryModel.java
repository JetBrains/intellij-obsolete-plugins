package com.intellij.plugins.jboss.arquillian.configuration.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryType;
import com.intellij.openapi.roots.libraries.PersistentLibraryKind;
import com.intellij.plugins.jboss.arquillian.ArquillianIcons;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianExistLibraryState;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianLibraryState;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ArquillianExistLibraryModel extends ArquillianLibraryModel {
  private final Project project;
  private final @NotNull @Nls String name;
  private final @NotNull String level;

  public ArquillianExistLibraryModel(Project project, ArquillianExistLibraryState state) {
    this.project = project;
    this.name = state.name;
    this.level = state.level;
  }

  @Override
  public boolean hasChanges(ArquillianLibraryState state) {
    if (!(state instanceof ArquillianExistLibraryState existLibraryState)) {
      return true;
    }
    return !name.equals(existLibraryState.name) || !level.equals(existLibraryState.level);
  }

  @Override
  public ArquillianExistLibraryState getCurrentState() {
    return new ArquillianExistLibraryState(name, level);
  }

  @Override
  public String getDescription() {
    return name;
  }

  @Override
  public Icon getIcon() {
    Library library = getCurrentState().findLibrary(project);
    if (library == null) {
      return null;
    }

    PersistentLibraryKind<?> kind = ((LibraryEx)library).getKind();
    if (kind == null) {
      return ArquillianIcons.Arquillian;
    }

    LibraryType libraryType = LibraryType.findByKind(kind);
    return libraryType.getIcon(((LibraryEx)library).getProperties());
  }

  @Override
  public void editProperties(Project project) {
  }
}
