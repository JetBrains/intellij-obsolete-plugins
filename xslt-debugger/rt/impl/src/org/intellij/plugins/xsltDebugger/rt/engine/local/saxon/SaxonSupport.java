// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.intellij.plugins.xsltDebugger.rt.engine.local.saxon;

import com.icl.saxon.Controller;
import com.icl.saxon.TransformerFactoryImpl;
import org.intellij.plugins.xsltDebugger.rt.engine.local.LocalDebugger;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

public final class SaxonSupport {
  public static boolean init(Transformer transformer, LocalDebugger dbg) {
    if (transformer instanceof Controller) {
      System.out.println("SAXON");
      final Controller controller = (Controller)transformer;
      controller.setLineNumbering(true);
      controller.addTraceListener(new SaxonTraceListener(dbg, controller));
      return true;
    }
    return false;
  }

  public static TransformerFactory createTransformerFactory() {
    return new TransformerFactoryImpl();
  }
}