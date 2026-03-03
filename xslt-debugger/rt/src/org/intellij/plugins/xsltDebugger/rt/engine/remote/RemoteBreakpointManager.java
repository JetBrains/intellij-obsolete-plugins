// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.intellij.plugins.xsltDebugger.rt.engine.remote;

import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface RemoteBreakpointManager extends Remote {
  RemoteBreakpoint setBreakpoint(File file, int line) throws RemoteException;

  RemoteBreakpoint setBreakpoint(String uri, int line) throws RemoteException;

  void removeBreakpoint(String uri, int line) throws RemoteException;

  List<RemoteBreakpoint> getBreakpoints() throws RemoteException;

  RemoteBreakpoint getBreakpoint(String uri, int lineNumber) throws RemoteException;
}
