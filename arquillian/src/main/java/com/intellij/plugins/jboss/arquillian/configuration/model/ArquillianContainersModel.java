package com.intellij.plugins.jboss.arquillian.configuration.model;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.jboss.arquillian.configuration.ArquillianContainersAppManager;
import com.intellij.plugins.jboss.arquillian.configuration.container.ArquillianContainer;
import com.intellij.plugins.jboss.arquillian.configuration.container.ArquillianContainerManual;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianContainerState;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianContainersState;
import org.jetbrains.annotations.NotNull;

import static com.intellij.plugins.jboss.arquillian.configuration.container.ArquillianContainerManual.MANUAL_CONTAINER_ID;

public class ArquillianContainersModel
  extends ArquillianListModel<ArquillianContainerState, ArquillianContainerModel, ArquillianContainersState, ArquillianContainersModel> {

  public ArquillianContainersModel(final Project project, ArquillianContainersState containersState) {
    super(containersState, new ArquillianModelCreator<>() {
      @Override
      public ArquillianContainerModel createModel(ArquillianContainerState containerState) {
        ArquillianContainer arquillianContainer = ArquillianContainersAppManager.getInstance()
          .findContainerById(containerState.getContainerId());
        return new ArquillianContainerModel(project, containerState, arquillianContainer);
      }
    });
  }

  @Override
  public ArquillianContainersState getCurrentState() {
    return new ArquillianContainersState(getChildrenStates());
  }

  public ArquillianContainerModel cloneConfiguration(ArquillianContainerModel model) {
    ArquillianContainerState newContainerState = model.getCurrentState().clone();
    newContainerState.name = selectUnusedName(newContainerState.name);
    newContainerState.containerId = MANUAL_CONTAINER_ID;
    ArquillianContainerModel newModel = childModelCreator.createModel(newContainerState);
    addItem(newModel);
    return newModel;
  }

  public String selectUnusedName(String name) {
    String suffix = "";
    int suffixIndex = 0;
    String suspectedName;
    do {
      suspectedName = name + suffix;
      suffix = " (" + ++suffixIndex + ")";
    }
    while (isUsedName(suspectedName));
    return suspectedName;
  }

  private boolean isUsedName(@NotNull final String name) {
    return getChildren().stream().anyMatch(m -> name.equals(m.getName()));
  }


}
