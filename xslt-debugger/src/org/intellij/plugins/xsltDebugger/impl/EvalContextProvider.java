// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.intellij.plugins.xsltDebugger.impl;

import com.intellij.psi.xml.XmlElement;
import com.intellij.util.ArrayUtilRt;
import org.intellij.lang.xpath.context.ContextProvider;
import org.intellij.lang.xpath.context.ContextType;
import org.intellij.lang.xpath.context.NamespaceContext;
import org.intellij.lang.xpath.context.SimpleVariableContext;
import org.intellij.lang.xpath.context.VariableContext;
import org.intellij.lang.xpath.psi.XPathElement;
import org.intellij.lang.xpath.xslt.context.XsltContextProvider;
import org.intellij.plugins.xsltDebugger.rt.engine.Debugger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EvalContextProvider extends ContextProvider {
  private final List<? extends Debugger.Variable> myVariables;

  public EvalContextProvider(List<? extends Debugger.Variable> model) {
    myVariables = model;
  }

  @Override
  public @NotNull ContextType getContextType() {
    return XsltContextProvider.TYPE;
  }

  @Override
  public @Nullable XmlElement getContextElement() {
    return null;
  }

  @Override
  protected boolean isValid() {
    return true;
  }

  @Override
  public @Nullable NamespaceContext getNamespaceContext() {
    return null;
  }

  @Override
  public VariableContext getVariableContext() {
    return new SimpleVariableContext() {
      @Override
      public String @NotNull [] getVariablesInScope(XPathElement element) {
        final int size = myVariables.size();
        final ArrayList<String> vars = new ArrayList<>(size);
        for (Debugger.Variable myVariable : myVariables) {
          vars.add(myVariable.getName());
        }
        return ArrayUtilRt.toStringArray(vars);
      }
    };
  }

  @Override
  public @Nullable Set<QName> getAttributes(boolean forValidation) {
    return null; // TODO
  }

  @Override
  public @Nullable Set<QName> getElements(boolean forValidation) {
    return null; // TODO
  }
}
