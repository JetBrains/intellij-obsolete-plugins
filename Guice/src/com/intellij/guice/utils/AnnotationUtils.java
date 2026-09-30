// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.utils;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiVariable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UDeclaration;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UField;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UParameter;
import org.jetbrains.uast.UTypeReferenceExpression;
import org.jetbrains.uast.UVariable;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.UastUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.intellij.codeInsight.AnnotationUtil.CHECK_HIERARCHY;

public final class AnnotationUtils {
  public static @Nullable PsiElement findDefaultValue(@Nullable PsiAnnotation annotation) {
    return annotation != null ? annotation.findDeclaredAttributeValue(PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME) : null;
  }

  public static @Nullable UExpression findDefaultValue(@NotNull UAnnotation annotation) {
    return annotation.findDeclaredAttributeValue(PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME);
  }

  public static @Nullable PsiClass resolveDefaultClassValue(@NotNull UAnnotation annotation) {
    UExpression value = findDefaultValue(annotation);
    return value != null ? GuiceUtils.resolveClass(GuiceUtils.getBindingTypeFromExpression(value)) : null;
  }

  /**
   * Resolves the {@link PsiClass} referenced by the default {@code value} attribute of
   * {@code annotationFqn} on {@code psiClass} (such as {@code @ImplementedBy} or {@code @ProvidedBy}),
   * supporting both Java/Kotlin source declarations and compiled classes.
   */
  public static @Nullable PsiClass resolveAnnotationClassValue(@NotNull PsiClass psiClass, @NotNull String annotationFqn) {
    UClass uClass = UastContextKt.toUElement(psiClass, UClass.class);
    if (uClass != null) {
      UAnnotation uAnnotation = uClass.findAnnotation(annotationFqn);
      if (uAnnotation != null) {
        PsiClass resolved = resolveDefaultClassValue(uAnnotation);
        if (resolved != null) return resolved;
      }
    }
    PsiModifierList modifierList = psiClass.getModifierList();
    if (modifierList == null) return null;
    PsiAnnotation annotation = modifierList.findAnnotation(annotationFqn);
    if (annotation == null) return null;
    PsiElement defaultValue = findDefaultValue(annotation);
    if (defaultValue instanceof PsiClassObjectAccessExpression classObjectAccess &&
        classObjectAccess.getOperand().getType() instanceof PsiClassType classType) {
      return classType.resolve();
    }
    return null;
  }

  /**
   * Resolves the declaration that owns {@code annotation}, mapping Kotlin property-level
   * annotations back to their {@link UField} and primary-constructor parameter annotations
   * back to their {@link UParameter}.
   */
  public static @Nullable UDeclaration resolveAnnotatedDeclaration(@NotNull UAnnotation annotation) {
    UDeclaration decl = UastUtils.getParentOfType(annotation, UDeclaration.class);
    if (decl == null) return null;
    PsiElement sourcePsi = decl.getSourcePsi();
    if (sourcePsi != null && !(sourcePsi instanceof PsiMethod)) {
      UElement sourceUElement = UastContextKt.toUElement(sourcePsi);
      if (sourceUElement instanceof UParameter parameter) {
        return parameter;
      }
      if (decl instanceof UMethod) {
        UClass uClass = UastUtils.getParentOfType(annotation, UClass.class);
        if (uClass != null) {
          for (UField field : uClass.getFields()) {
            if (sourcePsi.equals(field.getSourcePsi())) {
              return field;
            }
          }
        }
      }
    }
    return decl;
  }

  /**
   * Collects all UAST annotations on {@code declaration}, including Kotlin property annotations
   * that are not attached to the synthetic light field.
   */
  public static @NotNull List<UAnnotation> collectDeclarationUAnnotations(@NotNull UDeclaration declaration) {
    List<UAnnotation> result = new ArrayList<>();
    Set<PsiElement> seenSourcePsi = new HashSet<>();
    for (UAnnotation anno : declaration.getUAnnotations()) {
      PsiElement sourcePsi = anno.getSourcePsi();
      if (sourcePsi == null || seenSourcePsi.add(sourcePsi)) {
        result.add(anno);
      }
    }
    PsiElement declSourcePsi = declaration.getSourcePsi();
    if (declSourcePsi != null) {
      for (PsiElement child : declSourcePsi.getChildren()) {
        UAnnotation direct = UastContextKt.toUElement(child, UAnnotation.class);
        if (direct != null) {
          PsiElement s = direct.getSourcePsi();
          if (s == null || seenSourcePsi.add(s)) result.add(direct);
          continue;
        }
        for (PsiElement grandChild : child.getChildren()) {
          UAnnotation nested = UastContextKt.toUElement(grandChild, UAnnotation.class);
          if (nested != null) {
            PsiElement s = nested.getSourcePsi();
            if (s == null || seenSourcePsi.add(s)) result.add(nested);
          }
        }
      }
    }
    return result;
  }

  public static boolean isAnnotated(@NotNull UDeclaration declaration, @NotNull String annotationFqn) {
    return isAnnotated(declaration, List.of(annotationFqn));
  }

  public static boolean isAnnotated(@NotNull UDeclaration declaration, @NotNull Collection<String> annotationFqns) {
    if (declaration.getJavaPsi() instanceof PsiModifierListOwner owner &&
        AnnotationUtil.isAnnotated(owner, annotationFqns, CHECK_HIERARCHY)) {
      return true;
    }
    for (UAnnotation anno : collectDeclarationUAnnotations(declaration)) {
      String fqn = anno.getQualifiedName();
      if (fqn != null && annotationFqns.contains(fqn)) {
        return true;
      }
    }
    return false;
  }

  public static @Nullable PsiElement getTypeAnchor(@NotNull UVariable variable) {
    UTypeReferenceExpression typeRef = variable.getTypeReference();
    if (typeRef != null && typeRef.getSourcePsi() != null) {
      return typeRef.getSourcePsi();
    }
    if (variable.getJavaPsi() instanceof PsiVariable psiVariable && psiVariable.getTypeElement() != null) {
      return psiVariable.getTypeElement();
    }
    UElement anchor = variable.getUastAnchor();
    return anchor != null ? anchor.getSourcePsi() : variable.getSourcePsi();
  }
}
