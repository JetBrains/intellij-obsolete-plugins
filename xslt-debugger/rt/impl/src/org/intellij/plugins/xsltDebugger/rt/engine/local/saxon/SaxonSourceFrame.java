// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.intellij.plugins.xsltDebugger.rt.engine.local.saxon;

import com.icl.saxon.om.Navigator;
import com.icl.saxon.om.NodeInfo;
import org.intellij.plugins.xsltDebugger.rt.engine.Debugger;

class SaxonSourceFrame extends AbstractSaxonFrame<Debugger.SourceFrame, NodeInfo> implements Debugger.SourceFrame {
  SaxonSourceFrame(Debugger.SourceFrame prev, NodeInfo element) {
    super(prev, element);
  }

  @Override
  public String getXPath() {
    return Navigator.getPath(myElement);
  }
}
