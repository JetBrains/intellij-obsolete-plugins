// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.config;

import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.PsiImplUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyTokenTypes;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrAssignmentExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrLiteralImpl;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class GroovyConfigReader {

  private final Map<String, Map<String, PsiElement>> myEnvMap = new HashMap<>();

  private final Map<String, PsiElement> myDefaultEnv = new HashMap<>();

  private Map<String, PsiElement> myCurrentEnv = myDefaultEnv;

  private final StringBuilder myCurrentPrefixes = new StringBuilder();

  private GroovyConfigReader() {

  }

  private void processStatements(@NotNull PsiElement root) {
    for (PsiElement e = root.getFirstChild(); e != null; e = e.getNextSibling()) {
      if (e instanceof GrAssignmentExpression) {
        process((GrAssignmentExpression)e);
      }
      else if (e instanceof GrMethodCall) {
        process((GrMethodCall)e);
      }
    }
  }

  private void process(@NotNull GrAssignmentExpression assign) {
    if (assign.isOperatorAssignment()) return;

    GrExpression value = assign.getLValue();
    if (!(value instanceof GrReferenceExpression)) return;

    int prefixSize = myCurrentPrefixes.length();

    if (appendPropertyName(myCurrentPrefixes, (GrReferenceExpression)value)) {
      myCurrentEnv.put(myCurrentPrefixes.toString(), assign.getRValue());
    }

    myCurrentPrefixes.setLength(prefixSize);
  }

  private void process(@NotNull GrMethodCall call) {
    String methodName = PsiUtil.getUnqualifiedMethodName(call);
    if (methodName == null) return;

    GrClosableBlock closure = GrailsUtils.getClosureArgument(call);
    if (closure == null) return;

    if (myCurrentEnv == myDefaultEnv) {
      if (GrailsUtils.ENVIRONMENTS.equals(methodName)) {
        for (PsiElement envClosureChild = closure.getFirstChild();
             envClosureChild != null;
             envClosureChild = envClosureChild.getNextSibling()) {
          if (envClosureChild instanceof GrMethodCall mc) {

            String envName = PsiUtil.getUnqualifiedMethodName(mc);

            if (envName != null) {
              GrClosableBlock c = GrailsUtils.getClosureArgument(mc);
              if (c != null) {
                myCurrentEnv = myEnvMap.get(envName);
                if (myCurrentEnv == null) {
                  myCurrentEnv = new HashMap<>();
                  myEnvMap.put(envName, myCurrentEnv);
                }

                processStatements(c);

                myCurrentEnv = myDefaultEnv;
              }
            }
          }
        }

        return;
      }
    }

    int prefixSize = myCurrentPrefixes.length();

    if (!myCurrentPrefixes.isEmpty()) {
      myCurrentPrefixes.append('.');
    }
    myCurrentPrefixes.append(methodName);

    processStatements(closure);

    myCurrentPrefixes.setLength(prefixSize);
  }

  private static boolean appendPropertyName(StringBuilder buf, GrReferenceExpression ref) {
    GrExpression qualifier = ref.getQualifierExpression();
    if (qualifier != null) {
      if (!(qualifier instanceof GrReferenceExpression)) {
        return false;
      }

      if (!appendPropertyName(buf, (GrReferenceExpression)qualifier)) return false;
    }

    String refName;

    PsiElement nameElement = ref.getReferenceNameElement();
    if (nameElement instanceof GrLiteralImpl) {
      Object value = ((GrLiteralImpl)nameElement).getValue();
      if (!(value instanceof String)) return false;

      refName = (String)value;
    }
    else if (PsiImplUtil.isLeafElementOfType(nameElement, GroovyTokenTypes.mIDENT)) {
      refName = nameElement.getText();
    }
    else {
      return false;
    }

    if (!buf.isEmpty()) {
      buf.append('.');
    }

    buf.append(refName);

    return true;
  }

  public static GroovyConfigReader read(@NotNull GroovyFile groovyFile) {
    GroovyConfigReader res = new GroovyConfigReader();
    res.processStatements(groovyFile);
    return res;
  }

  public Set<String> getEnvironments() {
    return myEnvMap.keySet();
  }

  public @Nullable PsiElement getValue(@Nullable String environment, @NotNull String property) {
    if (environment != null) {
      Map<String, PsiElement> map = myEnvMap.get(environment);
      if (map != null) {
        PsiElement res = map.get(property);
        if (res != null) return res;
      }
    }

    return myDefaultEnv.get(property);
  }

  public @Nullable String getStringValue(@Nullable String environment, @NotNull String property) {
    PsiElement element = getValue(environment, property);
    if (element == null) return null;

    if (element instanceof GrLiteralImpl) {
      Object value = ((GrLiteralImpl)element).getValue();
      if (value instanceof String) return (String)value;

      return null;
    }

    return null;
  }
}
