package com.intellij.plugins.jboss.arquillian.configuration.container;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.jboss.arquillian.ArquillianBundle;
import com.intellij.plugins.jboss.arquillian.configuration.model.ArquillianContainerModel;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianContainerState;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.function.Supplier;

public abstract class ArquillianContainer {
  @NotNull
  public abstract String getId();

  @Nls(capitalization = Nls.Capitalization.Title)
  @NotNull
  public abstract String getName();

  @NotNull
  public abstract Scope getScope();

  @NotNull
  public abstract Icon getIcon();

  @Nullable
  public abstract String getDescriptionUri();

  @NotNull
  public abstract ArquillianContainerState createDefaultState(Project project, String name);

  @NotNull
  public abstract List<ArquillianMavenCoordinates> getDependencies();

  public abstract List<ArquillianContainerParameter> getParameters();

  public abstract JPanel createSettingsPanel(Project project, ArquillianContainerModel model);

  public abstract boolean canChangeDependencyList();

  public enum Scope implements Comparable<Scope> {
    Manual(ArquillianBundle.messagePointer("arquillian.manual.configuration.name"), 1000),
    Embedded(ArquillianBundle.messagePointer("arquillian.embedded.configuration.name"), 2000),
    Managed(ArquillianBundle.messagePointer("arquillian.managed.configuration.name"), 3000),
    Remote(ArquillianBundle.messagePointer("arquillian.remote.configuration.name"), 4000);

    private final Supplier<@Nls String> description;
    private final int weight;

    Scope(Supplier<@Nls String> description, int weight) {
      this.description = description;
      this.weight = weight;
    }

    public Supplier<@Nls String> getDescription() {
      return description;
    }

    public int getWeight() {
      return weight;
    }
  }
}
