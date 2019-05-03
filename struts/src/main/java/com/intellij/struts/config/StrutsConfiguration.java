/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

/**
 * @author Dmitry Avdeev
 */
package com.intellij.struts.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

@State(name = "StrutsConfiguration", storages = @Storage("other.xml"))
public class StrutsConfiguration implements PersistentStateComponent<StrutsConfiguration> {

  @Override
  public StrutsConfiguration getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull StrutsConfiguration state) {
    autoscrollFromSource = state.autoscrollFromSource;
    autoscrollToSource = state.autoscrollToSource;
  }

  public static StrutsConfiguration getInstance() {
    return ServiceManager.getService(StrutsConfiguration.class);
  }

  public boolean autoscrollToSource = false;

  public boolean autoscrollFromSource = false;
}
