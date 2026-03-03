// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.intellij.plugins.xsltDebugger.rt.engine;

import java.util.List;

public interface Debugger extends Watchable {
  enum State {
    CREATED, RUNNING, SUSPENDED, STOPPED
  }

  State getState();

  boolean start();

  void stop(boolean force);

  void step();

  void stepInto();

  void resume();

  void pause();

  boolean isStopped();

  StyleFrame getCurrentFrame();

  SourceFrame getSourceFrame();

  Value eval(String expr) throws EvaluationException;

  List<Variable> getGlobalVariables();

  BreakpointManager getBreakpointManager();

  OutputEventQueue getEventQueue();

  boolean waitForDebuggee();

  State waitForStateChange(State state);

  interface Locatable {
    String getURI();

    int getLineNumber();
  }

  interface Frame<T extends Frame> extends Locatable {
    T getNext();

    T getPrevious();
  }

  interface StyleFrame extends Frame<StyleFrame> {
    String getInstruction();

    Value eval(String expr) throws EvaluationException;

    List<Variable> getVariables();
  }

  interface SourceFrame extends Frame<SourceFrame> {
    String getXPath();
  }

  interface Variable extends Locatable {
    enum Kind {VARIABLE, PARAMETER, EXPRESSION}

    boolean isGlobal();

    Kind getKind();

    String getName();

    Value getValue();
  }

  class EvaluationException extends Exception {
    public EvaluationException(String message) {
      super(message);
    }
  }
}
