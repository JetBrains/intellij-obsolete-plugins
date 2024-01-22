package com.intellij.plugins.jboss.arquillian.configuration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.plugins.jboss.arquillian.configuration.container.ArquillianContainer;
import com.intellij.plugins.jboss.arquillian.configuration.container.ArquillianContainerBean;
import com.intellij.plugins.jboss.arquillian.configuration.container.ArquillianContainerManual;
import com.intellij.plugins.jboss.arquillian.configuration.container.ArquillianContainerPredefined;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Service
public final class ArquillianContainersAppManager {
  private static final ExtensionPointName<ArquillianContainerBean> EP_NAME =
    new ExtensionPointName<>("com.intellij.jboss.arquillian.container");

  private final ArquillianContainer manualContainer = new ArquillianContainerManual();
  private final EnumMap<ArquillianContainer.Scope, List<ArquillianContainer>> containers = new EnumMap<>(ArquillianContainer.Scope.class);
  private final @NotNull Map<String, ArquillianContainer> registeredContainers;

  public ArquillianContainersAppManager() {
    containers.put(ArquillianContainer.Scope.Manual, List.of(manualContainer));

    List<ArquillianContainerBean> toSort = new ArrayList<>(EP_NAME.getExtensionList());
    toSort.sort(Comparator.comparing(left -> left.name));
    for (ArquillianContainerBean bean : toSort) {
      ArquillianContainerPredefined predefined = new ArquillianContainerPredefined(bean);
      containers.computeIfAbsent(predefined.getScope(), __ -> new ArrayList<>()).add(predefined);
    }

    Map<String, ArquillianContainer> map = new HashMap<>();
    for (List<ArquillianContainer> arquillianContainers : containers.values()) {
      for (ArquillianContainer container : arquillianContainers) {
        if (map.put(container.getId(), container) != null) {
          throw new IllegalStateException("Duplicate key");
        }
      }
    }
    registeredContainers = Collections.unmodifiableMap(map);
  }

  public static ArquillianContainersAppManager getInstance() {
    return ApplicationManager.getApplication().getService(ArquillianContainersAppManager.class);
  }

  public @NotNull List<ArquillianContainer> getContainers(ArquillianContainer.Scope scope) {
    return Collections.unmodifiableList(containers.get(scope));
  }

  public @NotNull ArquillianContainer findContainerById(String id) {
    return registeredContainers.getOrDefault(id, manualContainer);
  }
}
