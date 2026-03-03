// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.intellij.plugins.xsltDebugger.rt.engine;

class BreakpointImpl implements Breakpoint {
  private final String myUri;
  private final int myLine;
  private boolean myEnabled;
  private String myCondition;
  private String myLogMsg;
  private String myTraceMsg;
  private boolean mySuspend;

  BreakpointImpl(String uri, int line) {
    myUri = uri;
    myLine = line;
    myEnabled = true;
  }

  @Override
  public void setEnabled(boolean b) {
    myEnabled = b;
  }

  @Override
  public void setCondition(String expr) {
    myCondition = expr;
  }

  @Override
  public void setLogMessage(String expr) {
    myLogMsg = expr;
  }

  @Override
  public String getTraceMessage() {
    return myTraceMsg;
  }

  @Override
  public void setTraceMessage(String expr) {
    myTraceMsg = expr;
  }

  @Override
  public boolean isSuspend() {
    return mySuspend;
  }

  @Override
  public void setSuspend(boolean suspend) {
    mySuspend = suspend;
  }

  @Override
  public String getCondition() {
    return myCondition;
  }

  @Override
  public String getLogMessage() {
    return myLogMsg;
  }

  @Override
  public String getUri() {
    return myUri;
  }

  @Override
  public int getLine() {
    return myLine;
  }

  @Override
  public boolean isEnabled() {
    return myEnabled;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final BreakpointImpl that = (BreakpointImpl)o;

    if (myLine != that.myLine) return false;
    return myUri.equals(that.myUri);
  }

  @Override
  public int hashCode() {
    int result;
    result = myUri.hashCode();
    result = 31 * result + myLine;
    return result;
  }


  @Override
  public String toString() {
    return "Breakpoint{" +
           "myUri='" + myUri + '\'' +
           ", myLine=" + myLine +
           ", myEnabled=" + myEnabled +
           '}';
  }
}
