// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.controller;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.GroovyMvcIcons;
import org.jetbrains.plugins.grails.util.GrailsUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class ActionReference extends PsiReferenceBase<PsiElement> implements Function<PsiElement, Map<String, PsiMethod>> {

  private final Function<? super PsiElement, ? extends Map<String, PsiMethod>> myControllerResolver;

  private final String myControllerName;

  private Map<String, PsiMethod> myActions;

  public ActionReference(PsiElement element,
                         TextRange range,
                         boolean soft,
                         Function<? super PsiElement, ? extends Map<String, PsiMethod>> controllerResolver) {
    super(element, range, soft);

    TextRange textRange = trimExtension(range);
    setRangeInElement(textRange);

    myControllerResolver = controllerResolver;
    myControllerName = null;
  }

  public ActionReference(PsiElement element, boolean soft, Function<? super PsiElement, ? extends Map<String, PsiMethod>> controllerResolver) {
    super(element, soft);
    myControllerResolver = controllerResolver;
    myControllerName = null;
  }

  public ActionReference(PsiElement element, boolean soft, @NotNull String controllerName) {
    super(element, soft);
    myControllerResolver = this;
    myControllerName = controllerName;
  }

  private TextRange trimExtension(TextRange defaultRange) {
    String elementText = getElement().getText();

    String value = defaultRange.substring(elementText);

    int dotIndex = value.lastIndexOf('.');
    if (dotIndex >= 0) {
      return TextRange.from(defaultRange.getStartOffset(), dotIndex);
    }

    return defaultRange;
  }

  @Override
  protected TextRange calculateDefaultRangeInElement() {
    TextRange defaultRange = super.calculateDefaultRangeInElement();
    defaultRange = trimExtension(defaultRange);
    return defaultRange;
  }

  @Override
  public PsiElement resolve() {
    String value = getValue();
    if (value.isEmpty()) return null;

    return GrailsUtils.toField(getActions().get(value));
  }

  public static LookupElementBuilder[] createLookupItems(Collection<String> actionNames) {
    LookupElementBuilder[] res = new LookupElementBuilder[actionNames.size()];

    int i = 0;

    for (String actionName : actionNames) {
      res[i++] = LookupElementBuilder.create(actionName).withIcon(GroovyMvcIcons.Action_method);
    }

    return res;
  }

  @Override
  public Object @NotNull [] getVariants() {
    return createLookupItems(getActions().keySet());
  }

  private Map<String, PsiMethod> getActions() {
    Map<String, PsiMethod> res = myActions;
    if (res == null) {
      res = myControllerResolver.fun(getElement());
      myActions = res;
    }

    return res;
  }

  public @Nullable String getControllerName() {
    return myControllerName;
  }

  @Override
  public Map<String, PsiMethod> fun(PsiElement psiElement) {
    Module module = ModuleUtilCore.findModuleForPsiElement(getElement());
    if (module == null) return Collections.emptyMap();

    return GrailsUtils.getControllerActions(myControllerName, module);
  }
}
