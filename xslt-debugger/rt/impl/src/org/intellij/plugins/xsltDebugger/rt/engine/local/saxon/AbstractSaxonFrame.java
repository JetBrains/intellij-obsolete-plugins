// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.intellij.plugins.xsltDebugger.rt.engine.local.saxon;

import com.icl.saxon.om.NodeInfo;
import org.intellij.plugins.xsltDebugger.rt.engine.Debugger;
import org.intellij.plugins.xsltDebugger.rt.engine.local.AbstractFrame;

class AbstractSaxonFrame<F extends Debugger.Frame, N extends NodeInfo> extends AbstractFrame<F> {
  protected final N myElement;

  protected AbstractSaxonFrame(F prev, N element) {
    super(prev);
    myElement = element;
  }

  @Override
  public int getLineNumber() {
    return myElement.getLineNumber();
  }

  @Override
  public String getURI() {
    final String uri = myElement.getSystemId();
    return uri != null ? uri.replaceAll(" ", "%20") : null;
  }
}
