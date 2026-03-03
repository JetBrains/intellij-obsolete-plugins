// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.intellij.plugins.xsltDebugger.rt.engine.local;

import org.intellij.plugins.xsltDebugger.rt.engine.Debugger;
import org.intellij.plugins.xsltDebugger.rt.engine.Value;

public class VariableImpl implements Debugger.Variable {
  private final boolean myGlobal;
  private final Kind myKind;
  private final String myRealname;
  private final Value myValue;
  private final String myUri;
  private final int myLineNumber;

  public VariableImpl(String realname, Value value, boolean global, Kind kind, String uri, int lineNumber) {
    myValue = value;
    myRealname = realname;
    myGlobal = global;
    myKind = kind;
    myUri = uri;
    myLineNumber = lineNumber;
  }

  @Override
  public String getURI() {
    return myUri;
  }

  @Override
  public int getLineNumber() {
    return myLineNumber;
  }

  @Override
  public String getName() {
    return myRealname;
  }

  @Override
  public Value getValue() {
    return myValue;
  }

  @Override
  public boolean isGlobal() {
    return myGlobal;
  }

  @Override
  public Kind getKind() {
    return myKind;
  }

  @Override
  public String toString() {
    return (myGlobal ? "global:" : "") + "{" + myKind + ":" + myRealname + "=" + myValue + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final VariableImpl that = (VariableImpl)o;

    return myRealname.equals(that.myRealname);
  }

  @Override
  public int hashCode() {
    return myRealname.hashCode();
  }
}
