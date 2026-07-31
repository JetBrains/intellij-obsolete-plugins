package com.intellij.gwt.make;

import com.intellij.gwt.facet.GwtFacet;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gwt.model.impl.GwtCompilerWorkspaceState;
import org.jetbrains.jps.gwt.model.impl.JpsGwtModelSerializerExtension;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@State(
  name = JpsGwtModelSerializerExtension.GWT_COMPILER_CONFIGURATION_COMPONENT_NAME,
  storages = {@Storage(StoragePathMacros.WORKSPACE_FILE)}
)
public final class GwtCompilerWorkspaceConfigurationImpl extends GwtCompilerWorkspaceConfiguration implements PersistentStateComponent<GwtCompilerWorkspaceState> {
  private final Set<String> myModulesToShowCompilerOutput = new HashSet<>();


  @Override
  public GwtCompilerWorkspaceState getState() {
    final GwtCompilerWorkspaceState state = new GwtCompilerWorkspaceState();
    state.getModulesToShowCompilerOutput().addAll(myModulesToShowCompilerOutput);
    Collections.sort(state.getModulesToShowCompilerOutput());
    return state;
  }

  @Override
  public void loadState(@NotNull GwtCompilerWorkspaceState state) {
    myModulesToShowCompilerOutput.clear();
    myModulesToShowCompilerOutput.addAll(state.getModulesToShowCompilerOutput());
  }

  @Override
  public boolean isShowCompilerOutput(@NotNull GwtFacet facet) {
    return myModulesToShowCompilerOutput.contains(facet.getModule().getName());
  }

  @Override
  public void setShowCompilerOutput(@NotNull GwtFacet facet, boolean show) {
    final String name = facet.getModule().getName();
    if (show) {
      myModulesToShowCompilerOutput.add(name);
    }
    else {
      myModulesToShowCompilerOutput.remove(name);
    }
  }
}
