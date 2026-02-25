// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.actions;

import com.intellij.codeInsight.CodeInsightUtilCore;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.config.GrailsStructure;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspFile;
import org.jetbrains.plugins.grails.references.controller.ActionReference;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariableDeclaration;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrCodeBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrAccessorMethod;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMembersDeclaration;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyNamesUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public final class GrailsCreateActionByUsageIntention implements IntentionAction {
  @Override
  public @NotNull String getText() {
    return GrailsBundle.message("intention.text.create.action");
  }

  @Override
  public @NotNull String getFamilyName() {
    return getText();
  }

  public static @Nullable Pair<GrClassDefinition, String> getData(Editor editor, PsiFile file) {
    if (!(file instanceof GroovyFileBase || file instanceof GspFile)) return null;

    PsiReference ref = file.findReferenceAt(editor.getCaretModel().getOffset());

    if (ref instanceof PsiMultiReference) {
      for (PsiReference reference : ((PsiMultiReference)ref).getReferences()) {
        if (reference instanceof ActionReference) {
          ref = reference;
          break;
        }
      }
    }

    if (!(ref instanceof ActionReference)) return null;
    if (ref.resolve() != null) return null;

    String controllerName = ((ActionReference)ref).getControllerName();

    if (controllerName == null) return null;

    Module module = ModuleUtilCore.findModuleForPsiElement(file);
    if (module == null) return null;

    Collection<GrClassDefinition> instances = GrailsArtifact.CONTROLLER.getInstances(module, controllerName);
    if (instances.isEmpty()) return null;

    String actionName = ((ActionReference)ref).getValue();
    if (!GroovyNamesUtil.isIdentifier(actionName)) return null;

    return Pair.create(instances.iterator().next(), actionName);
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile psiFile) {
    Pair<GrClassDefinition, String> pair = getData(editor, psiFile);

    return pair != null;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile psiFile) throws IncorrectOperationException {
    Pair<GrClassDefinition, String> pair = getData(editor, psiFile);
    if (pair == null) return;

    Module module = ModuleUtilCore.findModuleForPsiElement(psiFile);
    assert module != null;

    Map<String,PsiMethod> actions = GrailsUtils.getControllerActions(Collections.singletonList(pair.first), module);
    PsiMethod lastActionMethod = ContainerUtil.iterateAndGetLastItem(actions.values());

    boolean actionAsClosure = lastActionMethod instanceof GrAccessorMethod || !GrailsStructure.isAtLeastGrails1_4(module);

    GrMembersDeclaration action;

    if (actionAsClosure) {
      action = GroovyPsiElementFactory.getInstance(project).createFieldDeclarationFromText(
        "def " + pair.second + " = {}");
    }
    else {
      action = GroovyPsiElementFactory.getInstance(project).createMethodFromText("def " + pair.second + "() {}");
    }

    if (!CommonRefactoringUtil.checkReadOnlyStatus(project, pair.first)) {
      return;
    }

    if (lastActionMethod == null) {
      action = (GrMembersDeclaration)pair.first.addBefore(action, null);
    }
    else {
      PsiElement anchor = lastActionMethod;
      if (anchor instanceof GrAccessorMethod) {
        anchor = ((GrAccessorMethod)anchor).getProperty().getParent();
      }

      action = (GrMembersDeclaration)pair.first.addAfter(action, anchor);
    }

    action = CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(action);

    GrCodeBlock block;

    if (actionAsClosure) {
      block = (GrClosableBlock)((GrVariableDeclaration)action).getVariables()[0].getInitializerGroovy();
    }
    else {
      block = ((GrMethod)action).getBlock();
    }

    ((Navigatable)block.getLastChild()).navigate(true);
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }
}
