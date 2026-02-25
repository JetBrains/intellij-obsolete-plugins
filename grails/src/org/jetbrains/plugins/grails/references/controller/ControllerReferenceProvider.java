// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.controller;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.impl.light.LightElement;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GrMapAttributeValue;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GspOuterHtmlElement;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentLabel;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrConditionalExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrLiteralImpl;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ControllerReferenceProvider extends PsiReferenceProvider {

  private static final List<String> CONTROLLER_METHODS = Arrays.asList("chain", "forward", "redirect", "render");

  private static final Pattern URL_ATTR_PATTERN = Pattern.compile(".*\\s+url\\s*=\\s*['\"]", Pattern.DOTALL);

  private static final Map<String, Collection<String>> METHODS_WITH_PARAMETER_CONTROLLERACTION = GrailsUtils.createMap(
    "subflow", Collections.singleton("org.codehaus.groovy.grails.webflow.engine.builder.FlowInfoCapturer"),
    "render", Collections.singleton("org.codehaus.groovy.grails.webflow.engine.builder.FlowInfoCapturer"),
    "redirect", Collections.singleton("org.codehaus.groovy.grails.webflow.engine.builder.FlowInfoCapturer"),
    "link", Collections.singleton("org.codehaus.groovy.grails.web.mapping.LinkGenerator")
  );

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    PsiElement p = element.getParent();

    if (p instanceof GrConditionalExpression) {
      if (((GrConditionalExpression)p).getCondition() == p) return PsiReference.EMPTY_ARRAY;

      p = p.getParent();
    }

    if (!(p instanceof GrNamedArgument namedArgument)) return PsiReference.EMPTY_ARRAY;

    GrArgumentLabel label = namedArgument.getLabel();
    if (label == null) return PsiReference.EMPTY_ARRAY;

    String key = label.getName();

    boolean isAction = "action".equals(key);
    if (!isAction && !"controller".equals(key)) return PsiReference.EMPTY_ARRAY;

    PsiElement arguments = namedArgument.getParent();
    if (arguments instanceof GrListOrMap) {
      PsiElement parent = arguments.getParent();

      if (parent instanceof GrMapAttributeValue) {
        PsiElement prev = parent.getPrevSibling();
        if (!(prev instanceof GspOuterHtmlElement) || !URL_ATTR_PATTERN.matcher(prev.getText()).matches()) {
          return PsiReference.EMPTY_ARRAY;
        }

        PsiReference ref;

        if (isAction) {
          String controllerName = getNamedArgument(namedArgument, "controller");
          if (controllerName == null) {
            PsiFile psiFile = namedArgument.getContainingFile();
            if (psiFile == null) return PsiReference.EMPTY_ARRAY;

            VirtualFile file = psiFile.getOriginalFile().getVirtualFile();
            if (file == null) return PsiReference.EMPTY_ARRAY;

            controllerName = GrailsUtils.getControllerNameByGsp(file);
            if (controllerName == null) return PsiReference.EMPTY_ARRAY;
          }

          ref = new ActionReference(element, false, controllerName);
        }
        else {
          ref = new ControllerReference(element, false);
        }

        return new PsiReference[]{ref};

      }

      arguments = parent;
    }

    if (!(arguments instanceof GrArgumentList)) return PsiReference.EMPTY_ARRAY;

    PsiElement eMethodCall = arguments.getParent();
    if (!(eMethodCall instanceof GrMethodCall)) return PsiReference.EMPTY_ARRAY;

    if (!isSupportActionController((GrMethodCall)eMethodCall)) return PsiReference.EMPTY_ARRAY;

    PsiReference ref;

    if (isAction) {
      String controllerName = getNamedArgument(namedArgument, "controller");
      if (controllerName == null) {
        PsiClass artifactClass = PsiUtil.getContainingNotInnerClass(eMethodCall);
        GrailsArtifact artifact = GrailsArtifact.getType(artifactClass);

        if (artifact != GrailsArtifact.CONTROLLER) return PsiReference.EMPTY_ARRAY;

        assert artifactClass != null;
        controllerName = GrailsArtifact.CONTROLLER.getArtifactName(artifactClass);
      }

      ref = new ActionReference(element, false, controllerName);
    }
    else {
      ref = new ControllerReference(element, false);
    }

    return new PsiReference[]{ref};
  }

  private static boolean isSupportActionController(GrMethodCall methodCall) {
    GrExpression invokedExpr = methodCall.getInvokedExpression();

    if (!(invokedExpr instanceof GrReferenceExpression)) {
      return false;
    }

    String methodName = ((GrReferenceExpression)invokedExpr).getReferenceName();

    Collection<String> classes = METHODS_WITH_PARAMETER_CONTROLLERACTION.get(methodName);
    if (classes != null) {
      PsiMethod method = methodCall.resolveMethod();
      if (method != null) {
        PsiClass containingClass = method.getContainingClass();
        if (containingClass != null) {
          for (String aClass : classes) {
            if (InheritanceUtil.isInheritor(containingClass, aClass)) {
              return true;
            }
          }
        }
      }
    }

    if (CONTROLLER_METHODS.contains(methodName)) {
      PsiClass artifactClass = PsiUtil.getContainingNotInnerClass(methodCall);
      GrailsArtifact artifact = GrailsArtifact.getType(artifactClass);

      if (artifact == GrailsArtifact.CONTROLLER || artifact == GrailsArtifact.FILTER) {
        PsiMethod method = methodCall.resolveMethod();
        if (method instanceof LightElement) {
          return true;
        }
      }
    }

    return false;
  }

  private static @Nullable String getNamedArgument(GrNamedArgument oneNamedArgument, String argumentName) {
    PsiElement valueElement = PsiUtil.getNamedArgumentValue(oneNamedArgument, argumentName);
    if (valueElement == null) return null;

    if (valueElement instanceof GrLiteralImpl) {
      Object value = ((GrLiteral)valueElement).getValue();
      if (value instanceof String) return (String)value;
    }
    return ""; // Controller is defined but unknown.
  }
}
