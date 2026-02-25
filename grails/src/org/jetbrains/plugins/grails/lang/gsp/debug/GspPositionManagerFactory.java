// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.debug;

import com.intellij.debugger.PositionManager;
import com.intellij.debugger.PositionManagerFactory;
import com.intellij.debugger.engine.DebugProcess;
import org.jetbrains.annotations.NotNull;

public final class GspPositionManagerFactory extends PositionManagerFactory {
  @Override
  public PositionManager createPositionManager(@NotNull DebugProcess process) {
    return new GspPositionManager(process);
  }
}
