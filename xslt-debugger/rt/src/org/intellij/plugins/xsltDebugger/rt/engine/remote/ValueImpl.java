// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.intellij.plugins.xsltDebugger.rt.engine.remote;

import org.intellij.plugins.xsltDebugger.rt.engine.Value;

import java.io.Serializable;

class ValueImpl implements Value {
  private final Serializable myValue;
  private final Type myType;

  ValueImpl(Object value, Type type) {
    if (value instanceof Serializable) {
      myValue = (Serializable)value;
    } else {
      myValue = String.valueOf(value);
    }
    myType = type;
  }

  @Override
  public Serializable getValue() {
    return myValue;
  }

  @Override
  public Type getType() {
    return myType;
  }
}
