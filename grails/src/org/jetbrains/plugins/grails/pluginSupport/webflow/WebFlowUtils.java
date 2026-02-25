// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.pluginSupport.webflow;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiTypes;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.CachedValueProvider.Result;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.config.GrailsStructure;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightVariable;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class WebFlowUtils {

  public static final String FLOW_SUFFIX = "Flow";
  public static final String WEBFLOW = "webflow";

  private WebFlowUtils() {
  }

  public static boolean isWebFlowEnabled(@Nullable GrailsStructure structure) {
    if (structure == null) return false;
    if (structure.isAtLeastGrails("1.2")) {
      return structure.isPluginInstalled(WEBFLOW);
    }

    return true;
  }

  public static boolean isFlowActionField(GrField field) {
    String name = field.getName();
    if (!name.endsWith(FLOW_SUFFIX)) return false;

    if (GrailsUtils.getActionName(field) == null) return false;

    if (!isWebFlowEnabled(GrailsStructure.getInstance(field))) return false;

    return true;
  }

  public static @NotNull GrField getActionByStateDeclaration(GrMethodCall stateDeclaration) {
    PsiElement actionClosure = stateDeclaration.getParent();
    assert actionClosure instanceof GrClosableBlock;
    GrField res = (GrField)actionClosure.getParent();
    assert res.getName().endsWith("Flow");
    return res;
  }

  public static @NotNull String getStateNameByStateDeclaration(@NotNull GrMethodCall stateDeclaration) {
    return ((GrReferenceExpression)stateDeclaration.getInvokedExpression()).getReferenceName();
  }

  public static boolean isStateDeclaration(GrMethodCall methodCall, boolean checkParentField) {
    if (checkParentField) {
      PsiElement initClosure = methodCall.getParent();
      if (!(initClosure instanceof GrClosableBlock)) return false;

      PsiElement field = initClosure.getParent();
      if (!(field instanceof GrField)) return false;

      if (!isFlowActionField((GrField)field)) return false;
    }

    GrExpression[] allArguments = PsiUtil.getAllArguments(methodCall);
    if (allArguments.length > 1 || (allArguments.length == 1 && !(allArguments[0] instanceof GrClosableBlock))) return false;

    GrExpression ie = methodCall.getInvokedExpression();
    if (!(ie instanceof GrReferenceExpression)) return false;

    return !((GrReferenceExpression)ie).isQualified();
  }


  public static @NotNull Map<String, PsiVariable> getWebFlowStates(@NotNull GrField field) {
    return CachedValuesManager.getCachedValue(field, () -> {
      Map<String, PsiVariable> cachedValue;
      GrExpression initializer = field.getInitializerGroovy();
      if (initializer instanceof GrClosableBlock) {
        cachedValue = new HashMap<>();

        for (PsiElement e = initializer.getFirstChild(); e != null; e = e.getNextSibling()) {
          if (e instanceof GrMethodCall methodCall) {
            if (isStateDeclaration(methodCall, false)) {
              GrReferenceExpression ie = (GrReferenceExpression)methodCall.getInvokedExpression();
              String stateName = ie.getReferenceName();
              if (stateName != null) {
                PsiVariable var = new GrLightVariable(field.getManager(), stateName, PsiTypes.intType(), ie);
                cachedValue.put(stateName, var);
              }
            }
          }
        }
      }
      else {
        cachedValue = Collections.emptyMap();
      }

      return Result.create(cachedValue, PsiModificationTracker.MODIFICATION_COUNT);
    });
  }

  public static boolean isFlowInfoCapturerMethod(@Nullable PsiElement element) {
    if (element instanceof PsiMethod) {
      PsiClass containingClass = ((PsiMethod)element).getContainingClass();
      if (containingClass != null) {
        return "org.codehaus.groovy.grails.webflow.engine.builder.FlowInfoCapturer".equals(containingClass.getQualifiedName());
      }
    }

    return false;
  }

}
