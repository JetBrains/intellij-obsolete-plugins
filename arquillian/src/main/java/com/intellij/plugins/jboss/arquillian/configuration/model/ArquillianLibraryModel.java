package com.intellij.plugins.jboss.arquillian.configuration.model;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianLibraryState;
import org.jetbrains.annotations.Nls;

import javax.swing.*;

public abstract class ArquillianLibraryModel extends ArquillianModel<ArquillianLibraryState, ArquillianLibraryModel> {
  @Nls(capitalization = Nls.Capitalization.Sentence)
  public abstract String getDescription();

  public abstract Icon getIcon();

  public abstract void editProperties(Project project);
}

