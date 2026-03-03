// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.intellij.plugins.xsltDebugger.rt.engine.remote;

import org.intellij.plugins.xsltDebugger.rt.engine.Debugger;
import org.intellij.plugins.xsltDebugger.rt.engine.Value;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public final class RemoteVariableImpl extends UnicastRemoteObject implements RemoteDebugger.Variable {
  private final Debugger.Variable myVariable;

  RemoteVariableImpl(Debugger.Variable variable) throws RemoteException {
    myVariable = variable;
  }

  @Override
  public String getURI() {
    return myVariable.getURI();
  }

  @Override
  public int getLineNumber() {
    return myVariable.getLineNumber();
  }

  @Override
  public boolean isGlobal() {
    return myVariable.isGlobal();
  }

  @Override
  public Debugger.Variable.Kind getKind() {
    return myVariable.getKind();
  }

  @Override
  public String getName() {
    return myVariable.getName();
  }

  @Override
  public Value getValue() {
    final Value value = myVariable.getValue();
    return new ValueImpl(value.getValue(), value.getType());
  }

  public static List<RemoteDebugger.Variable> convert(List<? extends Debugger.Variable> list) throws RemoteException {
    List<RemoteDebugger.Variable> variables = new ArrayList<>(list.size());
    for (final Debugger.Variable variable : list) {
      variables.add(new RemoteVariableImpl(variable));
    }
    return variables;
  }
}
