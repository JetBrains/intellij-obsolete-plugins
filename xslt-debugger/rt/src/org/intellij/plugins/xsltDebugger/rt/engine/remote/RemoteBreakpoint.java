// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.intellij.plugins.xsltDebugger.rt.engine.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteBreakpoint extends Remote {
  String getUri() throws RemoteException;

  int getLine() throws RemoteException;

  boolean isEnabled() throws RemoteException;

  void setEnabled(boolean b) throws RemoteException;

  String getCondition() throws RemoteException;

  void setCondition(String expr) throws RemoteException;

  String getLogMessage() throws RemoteException;

  void setLogMessage(String expr) throws RemoteException;

  String getTraceMessage() throws RemoteException;

  void setTraceMessage(String expr) throws RemoteException;

  boolean isSuspend() throws RemoteException;

  void setSuspend(boolean suspend) throws RemoteException;
}
