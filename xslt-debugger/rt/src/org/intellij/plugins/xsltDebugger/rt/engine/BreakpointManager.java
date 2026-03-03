// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.intellij.plugins.xsltDebugger.rt.engine;

import java.io.File;
import java.util.List;

public interface BreakpointManager {
  Breakpoint setBreakpoint(File file, int line);

  Breakpoint setBreakpoint(String uri, int line);

  void removeBreakpoint(Breakpoint bp);

  void removeBreakpoint(String uri, int line);

  List<Breakpoint> getBreakpoints();

  Breakpoint getBreakpoint(String uri, int lineNumber);
}
