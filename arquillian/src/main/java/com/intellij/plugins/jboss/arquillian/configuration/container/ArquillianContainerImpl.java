package com.intellij.plugins.jboss.arquillian.configuration.container;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.jboss.arquillian.ArquillianIcons;
import com.intellij.plugins.jboss.arquillian.configuration.model.ArquillianContainerModel;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianContainerState;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianLibraryState;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianMavenLibraryState;
import com.intellij.plugins.jboss.arquillian.configuration.ui.ArquillianContainerSettings;
import com.intellij.util.Function;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class ArquillianContainerImpl extends ArquillianContainer {
  protected final String id;
  protected final @Nls String name;
  protected final String descriptionUri;
  protected final List<ArquillianMavenCoordinates> dependencies;

  public ArquillianContainerImpl(String idSuffix,
                                 @Nls(capitalization = Nls.Capitalization.Title) String name,
                                 String descriptionUri, ArquillianMavenCoordinates... dependencies) {
    this.descriptionUri = descriptionUri;
    this.id = "container.arquillian." + idSuffix;
    this.name = name;
    this.dependencies = Arrays.asList(dependencies);
  }

  public ArquillianContainerImpl(String idSuffix,
                                 @Nls(capitalization = Nls.Capitalization.Title) String name,
                                 String descriptionUri, List<ArquillianMavenCoordinates> dependencies) {
    this.descriptionUri = descriptionUri;
    this.id = "container.arquillian." + idSuffix;
    this.name = name;
    this.dependencies = dependencies;
  }

  @NotNull
  @Override
  public String getId() {
    return id;
  }

  @NotNull
  @Override
  public String getName() {
    return name;
  }

  @Nullable
  @Override
  public String getDescriptionUri() {
    return descriptionUri;
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return ArquillianIcons.Arquillian;
  }

  @NotNull
  @Override
  public List<ArquillianMavenCoordinates> getDependencies() {
    return dependencies;
  }

  @Override
  public List<ArquillianContainerParameter> getParameters() {
    return new ArrayList<>();
  }

  @NotNull
  @Override
  public ArquillianContainerState createDefaultState(Project project, String name) {
    ArquillianContainerState state = new ArquillianContainerState(getId(), name, JBIterable.from(dependencies).transform(
      (Function<ArquillianMavenCoordinates, ArquillianLibraryState>)coordinates -> new ArquillianMavenLibraryState(coordinates)).toList());
    ArquillianContainerUtils.loadDependenciesAsync(project, state);
    return state;
  }

  @Override
  public JPanel createSettingsPanel(Project project, ArquillianContainerModel model) {
    ArquillianContainerSettings settings = new ArquillianContainerSettings(project, this, model);
    return settings.getMainPanel();
  }
}