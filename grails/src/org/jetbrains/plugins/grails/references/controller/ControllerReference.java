// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.controller;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;

import java.util.Collection;
import java.util.Map;

public class ControllerReference extends PsiPolyVariantReferenceBase<PsiElement> {

  public ControllerReference(PsiElement psiElement, boolean soft) {
    super(psiElement, soft);
  }

  public ControllerReference(PsiElement element, TextRange range, boolean soft) {
    super(element, range, soft);
  }

  @Override
  public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
    String value = getValue();
    if (value.isEmpty()) return ResolveResult.EMPTY_ARRAY;

    Module module = ModuleUtilCore.findModuleForPsiElement(getElement());
    if (module == null) return ResolveResult.EMPTY_ARRAY;

    String name = StringUtil.decapitalize(StringUtil.trimEnd(value, "Controller"));
    Collection<GrClassDefinition> controllers = GrailsArtifact.CONTROLLER.getInstances(module, name);

    if (controllers.isEmpty()) return ResolveResult.EMPTY_ARRAY;

    ResolveResult[] res = new ResolveResult[controllers.size()];

    int i = 0;
    for (GrClassDefinition classDefinition : controllers) {
      res[i++] = new PsiElementResolveResult(classDefinition);
    }

    return res;
  }

  @Override
  public Object @NotNull [] getVariants() {
    Module module = ModuleUtilCore.findModuleForPsiElement(getElement());
    if (module == null) return ArrayUtilRt.EMPTY_OBJECT_ARRAY;

    MultiMap<String, GrClassDefinition> controllers = GrailsArtifact.CONTROLLER.getInstances(module);

    LookupElement[] res = new LookupElement[controllers.size()];

    int i = 0;
    for (Map.Entry<String, Collection<GrClassDefinition>> entry : controllers.entrySet()) {
      GrTypeDefinition controllerClass = entry.getValue().iterator().next();
      res[i++] = LookupElementBuilder.create(controllerClass, entry.getKey()).withIcon(AllIcons.Nodes.Controller);
    }

    return res;
  }

  @Override
  public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
    if (element instanceof PsiClass aClass) {
      if (GrailsArtifact.CONTROLLER.isInstance(aClass)) {
        String artifactName = GrailsArtifact.CONTROLLER.getArtifactName(aClass);
        if (getValue().equals(artifactName)) {
          return getElement();
        }
      }
    }
    return super.bindToElement(element);
  }

  @Override
  public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
    if (!newElementName.endsWith(GrailsArtifact.CONTROLLER.suffix)) {
      return getElement();
    }

    return super.handleElementRename(GrailsArtifact.CONTROLLER.getArtifactName(newElementName));
  }

}
