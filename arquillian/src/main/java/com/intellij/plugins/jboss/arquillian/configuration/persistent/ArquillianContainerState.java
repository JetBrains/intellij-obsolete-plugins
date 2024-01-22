package com.intellij.plugins.jboss.arquillian.configuration.persistent;

import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.jboss.arquillian.configuration.ArquillianContainersAppManager;
import com.intellij.plugins.jboss.arquillian.configuration.container.ArquillianContainer;
import com.intellij.plugins.jboss.arquillian.configuration.remoteDebugger.ConfigurationSpecificState;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.XCollection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag("container")
public class ArquillianContainerState implements ArquillianListState<ArquillianLibraryState>, Comparable<ArquillianContainerState> {
  @NotNull
  @Attribute("containerId")
  public String containerId;

  @NotNull
  @Attribute("name")
  public String name;

  @NotNull
  @Attribute("jvmParameters")
  public String jvmParameters;

  @NotNull
  @XCollection(elementTypes = {ArquillianMavenLibraryState.class, ArquillianExistLibraryState.class})
  @Property(surroundWithTag = false)
  public List<ArquillianLibraryState> libraries;

  @NotNull
  @XCollection(propertyElementName = "env-variables")
  public Map<String, String> envVariables;

  @NotNull
  @Property(surroundWithTag = false)
  @Tag("configurationSpecific")
  public ConfigurationSpecificState configurationSpecificState = new ConfigurationSpecificState();

  public ArquillianContainerState() {
    this("", "", new ArrayList<>());
  }

  public ArquillianContainerState(
    @NotNull String containerId,
    @NotNull String name,
    @NotNull List<ArquillianLibraryState> libraries) {
    this(containerId, name, libraries, "", new HashMap<>(), new ConfigurationSpecificState());
  }

  public ArquillianContainerState(
    @NotNull String containerId,
    @NotNull String name,
    @NotNull List<ArquillianLibraryState> libraries,
    @NotNull String jvmParameters,
    @NotNull Map<String, String> envVariables,
    @NotNull ConfigurationSpecificState configurationSpecificState) {
    this.containerId = containerId;
    this.name = name;
    this.libraries = libraries;
    this.jvmParameters = jvmParameters;
    this.envVariables = envVariables;
    this.configurationSpecificState = configurationSpecificState;
  }

  @NotNull
  public String getContainerId() {
    return containerId;
  }

  @NotNull
  @NlsSafe
  public String getName() {
    return name;
  }

  @NotNull
  public String getJvmParameters() {
    return jvmParameters;
  }

  @NotNull
  public Map<String, String> getEnvVariables() {
    return envVariables;
  }

  @NotNull
  public List<ArquillianLibraryState> getLibraries() {
    return libraries;
  }

  public void setLibraries(@NotNull List<ArquillianLibraryState> libraries) {
    this.libraries = libraries;
  }

  @NotNull
  public ConfigurationSpecificState getRemoteDebuggerState() {
    return configurationSpecificState;
  }

  @Override
  public ArquillianContainerState clone() {
    ArquillianContainerState result = new ArquillianContainerState();
    XmlSerializerUtil.copyBean(this, result);
    return result;
  }

  @Override
  public int compareTo(ArquillianContainerState other) {
    ArquillianContainer container = ArquillianContainersAppManager.getInstance().findContainerById(containerId);
    ArquillianContainer otherContainer = ArquillianContainersAppManager.getInstance().findContainerById(other.containerId);
    if (!container.equals(otherContainer)) {
      int weight = container.getScope().getWeight();
      int otherWeight = otherContainer.getScope().getWeight();
      if (weight != otherWeight) {
        return weight < otherWeight ? -1 : 1;
      }
    }
    return StringUtil.compare(getName(), other.getName(), true);
  }

  @Override
  public List<ArquillianLibraryState> getChildren() {
    return libraries;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArquillianContainerState state = (ArquillianContainerState)o;

    if (!containerId.equals(state.containerId)) return false;
    if (!name.equals(state.name)) return false;
    if (!jvmParameters.equals(state.jvmParameters)) return false;
    if (!libraries.equals(state.libraries)) return false;
    if (!envVariables.equals(state.envVariables)) return false;
    if (!configurationSpecificState.equals(state.configurationSpecificState)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = containerId.hashCode();
    result = 31 * result + name.hashCode();
    result = 31 * result + jvmParameters.hashCode();
    result = 31 * result + libraries.hashCode();
    result = 31 * result + envVariables.hashCode();
    result = 31 * result + configurationSpecificState.hashCode();
    return result;
  }
}
