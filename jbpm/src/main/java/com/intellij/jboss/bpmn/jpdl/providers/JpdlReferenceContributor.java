package com.intellij.jboss.bpmn.jpdl.providers;

import com.intellij.patterns.PsiExpressionPattern;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import org.jetbrains.annotations.NotNull;

import static com.intellij.patterns.PsiJavaPatterns.string;

public class JpdlReferenceContributor extends PsiReferenceContributor {
  private static final String CLASS_NAME = "org.jbpm.api.ExecutionService";
  private static final String[] METOD_NAMES =
    {"startProcessInstanceById", "startProcessInstanceByKey", "endProcessInstance", "deleteProcessInstance",
      "deleteProcessInstanceCascade"};

  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
    final PsiExpressionPattern.Capture<PsiExpression> psiExpression = PsiJavaPatterns.psiExpression()
      .methodCallParameter(0, PsiJavaPatterns.psiMethod().withName(string().oneOf(METOD_NAMES)).definedInClass(CLASS_NAME).
        withParameters(CommonClassNames.JAVA_LANG_STRING, ".."));

    registrar.registerReferenceProvider(psiExpression, new ProcessNamesReferenceProvider());
  }
}
