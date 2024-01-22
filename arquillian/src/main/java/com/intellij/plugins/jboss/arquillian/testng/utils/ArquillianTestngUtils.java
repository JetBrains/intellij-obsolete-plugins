package com.intellij.plugins.jboss.arquillian.testng.utils;

import com.intellij.execution.JavaExecutionUtil;
import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.plugins.jboss.arquillian.utils.ArquillianUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.util.PsiTreeUtil;
import com.theoryinpractice.testng.util.TestNGUtil;

public final class ArquillianTestngUtils {

  public static boolean isTestngArquillianEnabled(ConfigurationContext context) {
    Location<?> contextLocation = context.getLocation();
    assert contextLocation != null;

    if (!ArquillianUtils.hasArquillian(context.getProject())) {
      return false;
    }

    Location<?> location = JavaExecutionUtil.stepIntoSingleClass(contextLocation);
    if (location == null) return false;
    PsiClass testClass = PsiTreeUtil.getTopmostParentOfType(location.getPsiElement(), PsiClass.class);
    return testClass != null && TestNGUtil.isTestNGClass(testClass) && ArquillianUtils.isTestngArquillianEnabled(testClass);
  }
}
