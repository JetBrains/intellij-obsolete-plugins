/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.gutter;

import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.struts.StrutsManager;
import com.intellij.struts.StrutsModel;
import com.intellij.util.xml.DomElement;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Base class for JAVA-class annotators.
 *
 * @author Dmitry Avdeev
 */
abstract class ClassAnnotator {

  private final String myClassName;
  private final Icon myIcon;

  /**
   * Creates an annotator.
   *
   * @param className Name of the super-class.
   * @param icon      Gutter mark icon for navigation.
   */
  protected ClassAnnotator(@NotNull @NonNls final String className, @NotNull final Icon icon) {
    myClassName = className;
    myIcon = icon;
  }

  /**
   * Annotates the current class and creates a "Go To Declaration" gutter mark icon.
   *
   * @param clazz  Class to annotate.
   * @param holder Current AnnotationHolder.
   *
   * @return {@code true} if the current class was suitable for this Annotator, {@code false} otherwise.
   *
   * @see StrutsClassAnnotator#annotate(PsiElement,AnnotationHolder)
   */
  public boolean annotate(final PsiClass clazz, final AnnotationHolder holder) {

    // do not run on non-public or abstract classes
    if (!clazz.hasModifierProperty(PsiModifier.PUBLIC) ||
        clazz.hasModifierProperty(PsiModifier.ABSTRACT)) {
      return false;
    }

    final PsiClass superClass =
      JavaPsiFacade.getInstance(clazz.getProject()).findClass(myClassName, GlobalSearchScope.allScope(clazz.getProject()));
    if (superClass != null && clazz.isInheritor(superClass, true)) {
      final DomElement[] destinations = getDestinations(clazz);
      if (destinations == null) {
        final Annotation annotation = holder.createWarningAnnotation(clazz.getNameIdentifier(), "Unused declaration");
        annotation.setTextAttributes(CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES);
        // TODO create quickfix to register element in struts-config.xml
      } else {
        // tooltip text for ClassName
        final StringBuilder clazzToolTip = new StringBuilder(destinations[0].getPresentation().getTypeName()).append(" ");
        for (int i = 0; i < destinations.length; i++) {
          clazzToolTip.append(destinations[i].getPresentation().getElementName());
          if (i < destinations.length - 1) {
            clazzToolTip.append(" | ");
          }
        }

        final Annotation annotation = holder.createInfoAnnotation(clazz.getNameIdentifier(), clazzToolTip.toString());
        annotation.setGutterIconRenderer(new GotoDeclGutter(clazz, myIcon, null) {
          @Override
          protected DomElement[] getDestinations(@NotNull final PsiElement element) {
            return ClassAnnotator.this.getDestinations(clazz);
          }
        });
        return true; // indicate we ran succesfully
      }
    }
    return false;
  }

  @Nullable
  protected static StrutsModel getCombinedStrutsModel(final PsiClass clazz) {
    final Module module = ModuleUtilCore.findModuleForPsiElement(clazz);
    return StrutsManager.getInstance().getCombinedStrutsModel(module);
  }

  /**
   * Gets all navigation targets to display in the gutter mark popup.
   *
   * @param clazz Current class to get navigation targets for.
   * @return All navigation targets or {@code null} if none available.
   * @see GotoDeclGutter
   */
  @Nullable
  protected abstract DomElement[] getDestinations(final PsiClass clazz);

}