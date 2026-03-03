// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.intellij.plugins.xsltDebugger.rt.engine.local;

import org.intellij.plugins.xsltDebugger.rt.engine.Debugger.Frame;

public abstract class AbstractFrame<F extends Frame> implements Frame<F> {
  private final F myPrev;
  private F myNext;

  private boolean myValid = true;

  public AbstractFrame(F prev) {
    myPrev = prev;

    if (prev != null) {
      ((AbstractFrame)prev).myNext = this;
    }
  }

  public void invalidate() {
    assert myValid;
    assert myNext == null;
    if (myPrev != null) {
      ((AbstractFrame<?>)myPrev).myNext = null;
    }
    myValid = false;
  }

  @Override
  public F getNext() {
    assert myValid;
    return myNext;
  }

  @Override
  public F getPrevious() {
    assert myValid;
    return myPrev;
  }

  public boolean isValid() {
    return myValid;
  }

  protected static void debug(Throwable e) {
    assert _debug(e);
  }

  @SuppressWarnings("CallToPrintStackTrace")
  private static boolean _debug(Throwable e) {
    e.printStackTrace();
    return true;
  }
}
