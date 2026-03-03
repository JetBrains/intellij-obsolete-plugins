// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.intellij.plugins.xsltDebugger.rt.engine.remote;

import org.intellij.plugins.xsltDebugger.rt.engine.OutputEventQueue;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class RemoteEventQueueImpl extends UnicastRemoteObject implements RemoteDebugger.EventQueue {
  private final OutputEventQueue myQueue;

  public RemoteEventQueueImpl(OutputEventQueue queue) throws RemoteException {
    myQueue = queue;
  }

  @Override
  public List<OutputEventQueue.NodeEvent> getEvents() throws RemoteException {
    return myQueue.getEvents();
  }

  @Override
  public void setEnabled(boolean b) throws RemoteException {
    myQueue.setEnabled(b);
  }
}
