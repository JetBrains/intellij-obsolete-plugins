package com.intellij.plugins.jboss.arquillian.configuration.persistent;

import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.XCollection;

import java.util.ArrayList;
import java.util.List;

public class ArquillianContainersState implements ArquillianListState<ArquillianContainerState> {
  @XCollection(elementName = "container")
  @Property(surroundWithTag = false)
  public List<ArquillianContainerState> containers;

  public ArquillianContainersState() {
    this.containers = new ArrayList<>();
  }

  public ArquillianContainersState(List<ArquillianContainerState> containers) {
    this.containers = containers;
  }

  @Override
  public List<ArquillianContainerState> getChildren() {
    return containers;
  }
}
