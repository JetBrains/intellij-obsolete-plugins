// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.intellij.plugins.xsltDebugger.rt;

import org.intellij.plugins.xslt.run.rt.XSLTMain;
import org.intellij.plugins.xslt.run.rt.XSLTRunner;
import org.intellij.plugins.xsltDebugger.rt.engine.local.saxon.SaxonSupport;
import org.intellij.plugins.xsltDebugger.rt.engine.local.saxon9.Saxon9Support;
import org.intellij.plugins.xsltDebugger.rt.engine.local.xalan.XalanSupport;
import org.intellij.plugins.xsltDebugger.rt.engine.remote.DebuggerServer;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import java.rmi.RemoteException;


public class XSLTDebuggerMain implements XSLTMain {

  @Override
  public TransformerFactory createTransformerFactory() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    final String type = System.getProperty("xslt.transformer.type");
    if ("xalan".equalsIgnoreCase(type)) {
      return XalanSupport.createTransformerFactory();
    } else if ("saxon".equalsIgnoreCase(type)) {
      return SaxonSupport.createTransformerFactory();
    } else if ("saxon9".equalsIgnoreCase(type)) {
      return Saxon9Support.createTransformerFactory();
    } else if (type != null) {
      throw new UnsupportedOperationException("Unsupported Transformer type '" + type + "'");
    }
    return XalanSupport.prepareFactory(XSLTRunner.createTransformerFactoryStatic());
  }

  @Override
  public void start(Transformer transformer, Source source, Result result) throws TransformerException {
    try {
      DebuggerServer.create(transformer, source, result, Integer.getInteger("xslt.debugger.port"));
    } catch (RemoteException e) {
      throw new TransformerException(e.getMessage(), e.getCause());
    }
  }
}