// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.filter;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.Function;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.controller.ActionReference;
import org.jetbrains.plugins.grails.references.controller.ControllerReference;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentLabel;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition;
import org.jetbrains.plugins.groovy.lang.psi.api.util.GrNamedArgumentsOwner;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrLiteralImpl;
import org.jetbrains.plugins.groovy.lang.resolve.GroovyStringLiteralManipulator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilterReferenceProvider extends PsiReferenceProvider {

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    GrNamedArgument namedArgument = (GrNamedArgument)element.getParent();

    GrArgumentLabel label = namedArgument.getLabel();
    if (label == null) return PsiReference.EMPTY_ARRAY;

    String key = label.getName();

    boolean isAction = "action".equals(key);
    if (!isAction && !"controller".equals(key)) return PsiReference.EMPTY_ARRAY;

    PsiElement arguments = namedArgument.getParent();
    if (arguments instanceof GrListOrMap) {
      arguments = arguments.getParent();
    }

    if (!(arguments instanceof GrArgumentList)) return PsiReference.EMPTY_ARRAY;

    PsiElement eMethodCall = arguments.getParent();

    if (!GrailsFilterUtil.isFilterDefinitionMethod(eMethodCall)) return PsiReference.EMPTY_ARRAY;

    List<PsiReference> res = new ArrayList<>();

    String text = element.getText();

    assert element instanceof GrLiteralImpl;
    TextRange rangeInLiteral = GroovyStringLiteralManipulator.getLiteralRange(text);

    String value = rangeInLiteral.substring(text);

    int index = 0;

    while (index <= value.length()) {

      int end = index;
      if (index < value.length() && Character.isJavaIdentifierStart(value.charAt(index))) {
        do {
          end++;
        } while (end < value.length() && Character.isJavaIdentifierPart(value.charAt(end)));
      }

      TextRange range = new TextRange(rangeInLiteral.getStartOffset() + index, rangeInLiteral.getStartOffset() + end);

      index = end + 1;

      PsiReference ref;

      if (isAction) {
        ref = new ActionReference(element, range, false, ControllerResolver.INSTANCE);
      }
      else {
        ref = new ControllerReference(element, range, false);
      }

      res.add(ref);
    }

    return res.toArray(PsiReference.EMPTY_ARRAY);
  }

  private static final class ControllerResolver implements Function<PsiElement, Map<String, PsiMethod>> {

    private static final Function<PsiElement, Map<String, PsiMethod>> INSTANCE = new ControllerResolver();

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*");

    private ControllerResolver() {
    }

    @Override
    public Map<String, PsiMethod> fun(PsiElement element) {
      PsiElement parent = element.getParent();

      if (!(parent instanceof GrNamedArgument namedArgument)) return Collections.emptyMap();

      GrNamedArgument controllerNamedArgument = ((GrNamedArgumentsOwner)namedArgument.getParent()).findNamedArgument("controller");

      List<String> controllerNames = Collections.emptyList();

      if (controllerNamedArgument != null) {
        PsiElement expression = controllerNamedArgument.getExpression();
        if (expression instanceof GrLiteralImpl) {
          Object oText = ((GrLiteralImpl)expression).getValue();
          if (oText instanceof String) {
            Matcher matcher = IDENTIFIER_PATTERN.matcher((String)oText);

            controllerNames = new ArrayList<>();

            while (matcher.find()) {
              controllerNames.add(matcher.group());
            }
          }
        }
      }

      Module module = ModuleUtilCore.findModuleForPsiElement(element);
      if (module == null) return Collections.emptyMap();

      MultiMap<String,GrClassDefinition> multiMap = GrailsArtifact.CONTROLLER.getInstances(module);

      if (controllerNames.isEmpty()) {
        return GrailsUtils.getControllerActions(multiMap.values(), module);
      }

      List<GrClassDefinition> controllers = new ArrayList<>();

      for (String controllerName : controllerNames) {
        controllers.addAll(multiMap.get(controllerName));
      }

      return GrailsUtils.getControllerActions(controllers, module);
    }
  }
}
