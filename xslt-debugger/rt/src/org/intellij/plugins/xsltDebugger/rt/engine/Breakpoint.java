// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.intellij.plugins.xsltDebugger.rt.engine;

public interface Breakpoint {
  String getUri();

  int getLine();

  boolean isEnabled();

  void setEnabled(boolean enabled);

  String getCondition();

  void setCondition(String expr);

  String getLogMessage();

  void setLogMessage(String expr);

  String getTraceMessage();

  void setTraceMessage(String expr);

  boolean isSuspend();

  void setSuspend(boolean suspend);
}
