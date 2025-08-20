package com.intellij.plugins.jboss.arquillian.configuration.persistent;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.jboss.arquillian.configuration.model.ArquillianContainersModel;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@State(name = "ArquillianContainersManager", storages = @Storage("arquillianContainers.xml"))
public class ArquillianContainersManager implements PersistentStateComponent<ArquillianContainersState> {
  private ArquillianContainersState state = new ArquillianContainersState();

  @NotNull
  public static ArquillianContainersManager getInstance(Project project) {
    return project.getService(ArquillianContainersManager.class);
  }

  @NotNull
  @Override
  public ArquillianContainersState getState() {
    return state;
  }

  @Override
  public void loadState(@NotNull ArquillianContainersState state) {
    this.state = state;
  }

  public void saveContainersModel(ArquillianContainersModel containersModel) {
    state.containers = containersModel.getChildrenStates();
  }

  public boolean hasChanges(ArquillianContainersModel containersModel) {
    return containersModel.hasChanges(state);
  }

  public ArquillianContainerState findStateByName(final String name) {
    return JBIterable.from(state.containers)
      .filter(state1 -> Objects.equals(name, state1.getName()))
      .first();
  }
}
