// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.intellij.plugins.xsltDebugger.rt.engine.remote;

import org.intellij.plugins.xsltDebugger.rt.engine.Debugger;
import org.intellij.plugins.xsltDebugger.rt.engine.Debugger.Frame;
import org.intellij.plugins.xsltDebugger.rt.engine.Value;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public final class RemoteFrameImpl extends UnicastRemoteObject implements RemoteDebugger.Frame {
  private final Frame<?> myFrame;
  private final String myAccessToken;

  private RemoteFrameImpl(Frame<?> frame, String accessToken) throws RemoteException {
    myFrame = frame;
    myAccessToken = accessToken;
  }

  @Override
  public int getLineNumber() {
    return myFrame.getLineNumber();
  }

  @Override
  public String getURI() {
    return myFrame.getURI();
  }

  @Override
  public RemoteDebugger.Frame getNext() throws RemoteException {
    return create(myFrame.getNext(), myAccessToken);
  }

  @Override
  public RemoteDebugger.Frame getPrevious() throws RemoteException {
    return create(myFrame.getPrevious(), myAccessToken);
  }

  @Override
  public String getXPath() {
    return ((Debugger.SourceFrame)myFrame).getXPath();
  }

  @Override
  public ValueImpl eval(String expr, String accessToken) throws Debugger.EvaluationException, RemoteException {
    if (!myAccessToken.equals(accessToken)) throw new RemoteException("Access denied");
    final Value value = ((Debugger.StyleFrame)myFrame).eval(expr);
    return new ValueImpl(value.getValue(), value.getType());
  }

  @Override
  public List<RemoteDebugger.Variable> getVariables() throws RemoteException {
    return RemoteVariableImpl.convert(((Debugger.StyleFrame)myFrame).getVariables());
  }

  @Override
  public String getInstruction() {
    return ((Debugger.StyleFrame)myFrame).getInstruction();
  }

  public static RemoteFrameImpl create(Frame<?> frame, String accessToken) throws RemoteException {
    return frame != null ? new RemoteFrameImpl(frame, accessToken) : null;
  }
}
