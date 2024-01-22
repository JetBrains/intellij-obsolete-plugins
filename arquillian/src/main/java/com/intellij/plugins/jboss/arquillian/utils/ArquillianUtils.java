package com.intellij.plugins.jboss.arquillian.utils;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.execution.JavaExecutionUtil;
import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.junit.JUnitUtil;
import com.intellij.execution.junit2.info.LocationUtil;
import com.intellij.execution.testframework.AbstractJavaTestConfigurationProducer;
import com.intellij.java.library.JavaLibraryUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.plugins.jboss.arquillian.constants.ArquillianConstants;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.intellij.plugins.jboss.arquillian.constants.ArquillianConstants.ARQUILLIAN_CORE_MAVEN;
import static com.intellij.psi.PsiModifier.ABSTRACT;

public final class ArquillianUtils {
  public static boolean isJunitArquillianEnabled(ConfigurationContext context) {
    Location<?> contextLocation = context.getLocation();
    assert contextLocation != null;

    if (!hasArquillian(context.getProject())) {
      return false;
    }

    Location<?> location = JavaExecutionUtil.stepIntoSingleClass(contextLocation);
    if (location == null) return false;
    PsiClass testClass = JUnitUtil.getTestClass(location);
    return testClass != null && isJunitArquillianEnabled(testClass);
  }

  public static boolean hasArquillian(@NotNull Project project) {
    return JavaLibraryUtil.hasLibraryJar(project, ARQUILLIAN_CORE_MAVEN);
  }

  public static boolean isJunitArquillianEnabled(@NotNull PsiClass aClass) {
    PsiModifierList modifierList = aClass.getModifierList();
    if (aClass.isInterface() || modifierList != null && modifierList.hasExplicitModifier(ABSTRACT)) {
      return false;
    }
    PsiAnnotation runWithAnnotation = lookupRunWithAnnotation(aClass).first;
    return runWithAnnotation != null && isArquillianRunner(runWithAnnotation, aClass.getProject());
  }

  private static boolean isArquillianRunner(@NotNull PsiAnnotation runWithAnnotation, Project project) {
    for (PsiNameValuePair nameValuePair : runWithAnnotation.getParameterList().getAttributes()) {
      String name = nameValuePair.getName();
      if (name != null && !name.equals("value")) {
        continue;
      }
      PsiAnnotationMemberValue value = nameValuePair.getDetachedValue();
      if (!(value instanceof PsiClassObjectAccessExpression)) {
        return false;
      }
      PsiTypeElement typeElement = ((PsiClassObjectAccessExpression)value).getOperand();

      PsiType arquillianJunitType = PsiType.getTypeByName(
        ArquillianConstants.JUNIT_ARQUILLIAN_CLASS,
        project,
        GlobalSearchScope.allScope(project));

      return arquillianJunitType.isAssignableFrom(typeElement.getType());
    }
    return false;
  }

  private static Pair<PsiAnnotation, Boolean> lookupRunWithAnnotation(PsiClass aClass) {
    boolean isInherited = false;
    PsiAnnotation result = null;
    Set<PsiClass> visitedClasses = new HashSet<>();
    while (aClass != null && !visitedClasses.contains(aClass)) {
      visitedClasses.add(aClass);
      result = getArquillianAnnotation(aClass);
      if (result != null) {
        break;
      }
      isInherited = true;
      aClass = aClass.getSuperClass();
    }
    return Pair.create(result, isInherited);
  }

  private static PsiAnnotation getArquillianAnnotation(@NotNull PsiClass aClass) {
    PsiModifierList classModifierList = aClass.getModifierList();
    if (classModifierList == null) {
      return null;
    }
    return classModifierList.findAnnotation(ArquillianConstants.JUNIT_RUN_WITH_CLASS);
  }

  public static PsiElement getJunitArquillianEnabledElement(@NotNull PsiClass aClass) {
    if (!isJunitArquillianEnabled(aClass)) {
      return null;
    }
    PsiElement annotation = getArquillianAnnotation(aClass);
    return annotation != null ? annotation : aClass.getNameIdentifier();
  }

  public static PsiElement getTestngArquillianEnabledElement(@NotNull PsiClass aClass) {
    PsiClass psiClass = getTestngArquillianType(aClass.getProject()).resolve();
    return (psiClass != null && aClass.isInheritor(psiClass, true))
           ? aClass.getExtendsList()
           : null;
  }

  public static boolean isTestngArquillianEnabled(@NotNull PsiClass aClass) {
    PsiModifierList modifierList = aClass.getModifierList();
    if (modifierList != null && modifierList.hasExplicitModifier(ABSTRACT)) {
      return false;
    }
    PsiClass psiClass = getTestngArquillianType(aClass.getProject()).resolve();
    return psiClass != null && aClass.isInheritor(psiClass, true);
  }

  public static PsiClassType getTestngArquillianType(@NotNull Project project) {
    return PsiType.getTypeByName(
      ArquillianConstants.TESTNG_ARQUILLIAN_CLASS,
      project,
      GlobalSearchScope.allScope(project));
  }

  public static List<PsiMethod> getAnnotatedMethods(@NotNull PsiClass aClass, @NonNls @NotNull String annotationFQN) {
    List<PsiMethod> result = new ArrayList<>();
    for (PsiMethod method : aClass.getAllMethods()) {
      if (AnnotationUtil.isAnnotated(method, annotationFQN, 0)) {
        result.add(method);
      }
    }
    return result;
  }

  public static boolean isClassAvailableInContext(ConfigurationContext context, String fqn) {
    PsiPackage psiPackage = AbstractJavaTestConfigurationProducer.checkPackage(context.getPsiLocation());
    if (context.getLocation() == null || psiPackage == null) {
      return false;
    }
    if (!LocationUtil.isJarAttached(context.getLocation(), psiPackage, fqn)) {
      return false;
    }

    return true;
  }

}
