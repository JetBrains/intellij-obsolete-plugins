package com.intellij.plugins.jboss.arquillian.configuration.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.jboss.arquillian.configuration.container.ArquillianContainer;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianContainerState;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianExistLibraryState;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianLibraryState;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianMavenLibraryState;
import com.intellij.plugins.jboss.arquillian.configuration.remoteDebugger.RemoteDebuggerModel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class ArquillianContainerModel
  extends ArquillianListModel<ArquillianLibraryState, ArquillianLibraryModel, ArquillianContainerState, ArquillianContainerModel> {
  private final ArquillianContainer arquillianContainer;
  private @NotNull @Nls String name;
  private @NotNull String jvmParameters;
  @NotNull Map<String, String> envVariables = new HashMap<>();
  @NotNull RemoteDebuggerModel remoteDebuggerModel;

  public ArquillianContainerModel(Project project, String name, ArquillianContainer arquillianContainer) {
    this(project, arquillianContainer.createDefaultState(project, name), arquillianContainer);
  }

  public ArquillianContainerModel(Project project, ArquillianContainerState state, ArquillianContainer arquillianContainer) {
    super(state, new ArquillianModelCreator<>() {
      @Override
      public ArquillianLibraryModel createModel(ArquillianLibraryState libraryState) {
        return libraryState.accept(new ArquillianLibraryState.Visitor<>() {
          @Override
          public ArquillianLibraryModel visitMavenLibrary(ArquillianMavenLibraryState state) {
            return new ArquillianMavenLibraryModel(state);
          }

          @Override
          public ArquillianLibraryModel visitExistLibrary(ArquillianExistLibraryState state) {
            return new ArquillianExistLibraryModel(project, state);
          }
        });
      }
    });
    this.arquillianContainer = arquillianContainer;
    this.name = state.getName();
    this.jvmParameters = state.getJvmParameters();
    setEnvVariables(state.getEnvVariables());
    this.remoteDebuggerModel = new RemoteDebuggerModel(project, state.getRemoteDebuggerState());
    remoteDebuggerModel.addChangeListener(new ArquillianModel.Listener<>() {
      @Override
      public void itemChanged(RemoteDebuggerModel model) {
        notifyMeChanged();
      }
    });
  }

  @NotNull
  @Nls
  public String getName() {
    return name;
  }

  public void setName(@NotNull @NlsSafe String name) {
    this.name = name;
    notifyMeChanged();
  }

  @NotNull
  public String getJvmParameters() {
    return jvmParameters;
  }

  public void setJvmParameters(@NotNull String jvmParameters) {
    this.jvmParameters = jvmParameters;
    notifyMeChanged();
  }

  @NotNull
  public Map<String, String> getEnvVariables() {
    return envVariables;
  }

  public void setEnvVariables(@NotNull Map<String, String> envVariables) {
    this.envVariables.clear();
    this.envVariables.putAll(envVariables);
    notifyMeChanged();
  }

  @NotNull
  public RemoteDebuggerModel getRemoteDebuggerModel() {
    return remoteDebuggerModel;
  }

  @Nullable
  public Icon getIcon() {
    return arquillianContainer.getIcon();
  }

  @NotNull
  public ArquillianContainer getDescription() {
    return arquillianContainer;
  }

  @Override
  public boolean hasChanges(ArquillianContainerState state) {
    return !jvmParameters.equals(state.getJvmParameters())
           || !name.equals(state.getName())
           || !envVariables.equals(state.getEnvVariables())
           || remoteDebuggerModel.hasChanges(state.getRemoteDebuggerState())
           || super.hasChanges(state);
  }

  @Override
  public ArquillianContainerState getCurrentState() {
    return new ArquillianContainerState(
      arquillianContainer.getId(),
      name,
      getChildrenStates(),
      jvmParameters,
      envVariables,
      remoteDebuggerModel.getCurrentState());
  }

  public static class ScopeNameComparator implements Comparator<ArquillianContainerModel> {
    @Override
    public int compare(ArquillianContainerModel o1, ArquillianContainerModel o2) {
      ArquillianContainer.Scope scope1 = o1.getDescription().getScope();
      ArquillianContainer.Scope scope2 = o2.getDescription().getScope();
      return scope1 == scope2 ? StringUtil.compare(o1.getName(), o2.getName(), true) : scope1.compareTo(scope2);
    }
  }
}
