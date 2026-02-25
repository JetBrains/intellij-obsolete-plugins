// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.annotator;

import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiNamedElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.GroovyMvcIcons;
import org.jetbrains.plugins.grails.pluginSupport.webflow.WebFlowUtils;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.grails.util.ReferenceGutterIconRenderer;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;

import java.util.List;

public final class GrailsControllerAnnotator implements Annotator {
  @Override
  public void annotate(final @NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (GrailsUtils.isControllerAction(element)) {
      PsiElement annotatedElement = ((PsiNameIdentifierOwner)element).getNameIdentifier();
      if (annotatedElement == null) return;

      List<PsiFile> views = GrailsUtils.getViewPsiByAction(element);
      if (!views.isEmpty()) {
        NavigationGutterIconBuilder.create(GroovyMvcIcons.Gsp_logo).setTargets(views).setNamer(
          e -> e instanceof PsiNamedElement ? ((PsiNamedElement)e).getName() : null
        ).createGutterIcon(holder, annotatedElement);
      }
      return;
    }

    if (element instanceof GrMethodCall methodCall && WebFlowUtils.isStateDeclaration(methodCall, true)) {
      GrField actionDef = WebFlowUtils.getActionByStateDeclaration(methodCall);
      String actionName = GrailsUtils.getActionName(actionDef);

      VirtualFile controllerGspDir = GrailsUtils.getControllerGspDir(actionDef.getContainingClass());
      if (controllerGspDir != null) {
        VirtualFile actionDir = controllerGspDir.findChild(actionName);
        if (actionDir != null) {
          String stateName = WebFlowUtils.getStateNameByStateDeclaration(methodCall);
          VirtualFile gspVF = actionDir.findChild(stateName + ".gsp");
          if (gspVF != null) {
            PsiFile gspFile = actionDef.getManager().findFile(gspVF);
            if (gspFile != null) {
              createAnnotation(holder, methodCall.getInvokedExpression(), gspFile);
            }
          }
        }
      }
    }
  }

  private static void createAnnotation(AnnotationHolder holder, PsiElement element, @NotNull PsiFile gspFile) {
    holder.newSilentAnnotation(HighlightSeverity.INFORMATION).range(element).gutterIconRenderer(new ReferenceGutterIconRenderer(gspFile, GroovyMvcIcons.Gsp_logo, gspFile.getName())).create();
  }
}
