// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.intellij.plugins.xsltDebugger.rt.engine.local.saxon9;

import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.tree.util.Navigator;
import org.intellij.plugins.xsltDebugger.rt.engine.Debugger;

class Saxon9SourceFrame<N extends NodeInfo> extends AbstractSaxon9Frame<Debugger.SourceFrame, N> implements Debugger.SourceFrame {

  protected Saxon9SourceFrame(Debugger.SourceFrame prev, N element) {
    super(prev, element);
  }


  @Override
  public String getXPath() {
    return Navigator.getPath(myElement);
  }
}
