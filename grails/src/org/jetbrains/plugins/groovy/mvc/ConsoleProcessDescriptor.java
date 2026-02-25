// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.mvc;

import com.intellij.execution.process.ProcessListener;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;

public interface ConsoleProcessDescriptor {
  ConsoleProcessDescriptor addProcessListener(@NotNull ProcessListener listener);

  ConsoleProcessDescriptor waitWith(ProgressIndicator progressIndicator);
}
