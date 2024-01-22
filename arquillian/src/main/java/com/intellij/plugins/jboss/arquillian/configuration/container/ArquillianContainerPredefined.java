package com.intellij.plugins.jboss.arquillian.configuration.container;

import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

public class ArquillianContainerPredefined extends ArquillianContainerImpl {
  private final Scope scope;

  public ArquillianContainerPredefined(ArquillianContainerBean bean) {
    super(bean.id,
          bean.name,
          bean.url,
          ContainerUtil.map(bean.dependencies, dep ->
            new ArquillianMavenCoordinates(dep.groupId, dep.artifactId)));
    this.scope = bean.kind.getScope();
  }

  @NotNull
  @Override
  public Scope getScope() {
    return scope;
  }

  @Override
  public boolean canChangeDependencyList() {
    return false;
  }
}
