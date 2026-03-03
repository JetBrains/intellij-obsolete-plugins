// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.intellij.plugins.xsltDebugger.rt.engine.remote;

import org.intellij.plugins.xsltDebugger.rt.engine.BreakpointManager;

import java.io.File;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public final class RemoteBreakpointManagerImpl extends UnicastRemoteObject implements RemoteBreakpointManager {
  private final BreakpointManager myManager;

  public RemoteBreakpointManagerImpl(BreakpointManager manager) throws RemoteException {
    super();
    myManager = manager;
  }

  @Override
  public RemoteBreakpoint setBreakpoint(File file, int line) throws RemoteException {
    return RemoteBreakpointImpl.create(myManager.setBreakpoint(file, line));
  }

  @Override
  public RemoteBreakpoint setBreakpoint(String uri, int line) throws RemoteException {
    return RemoteBreakpointImpl.create(myManager.setBreakpoint(uri, line));
  }

  @Override
  public void removeBreakpoint(String uri, int line) {
    myManager.removeBreakpoint(uri, line);
  }

  @Override
  public List<RemoteBreakpoint> getBreakpoints() throws RemoteException {
    return RemoteBreakpointImpl.convert(myManager.getBreakpoints());
  }

  @Override
  public RemoteBreakpoint getBreakpoint(String uri, int lineNumber) throws RemoteException {
    return RemoteBreakpointImpl.create(myManager.getBreakpoint(uri, lineNumber));
  }
}
