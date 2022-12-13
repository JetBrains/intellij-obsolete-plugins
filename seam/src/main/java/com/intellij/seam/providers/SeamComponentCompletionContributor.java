package com.intellij.seam.providers;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PsiJavaElementPattern;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.codeStyle.SuggestedNameInfo;
import com.intellij.psi.codeStyle.VariableKind;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.seam.constants.SeamAnnotationConstants;
import org.jetbrains.annotations.NotNull;

import static com.intellij.patterns.PsiJavaPatterns.psiElement;

/**
 * @author Serega.Vasiliev
 */

public class SeamComponentCompletionContributor extends CompletionContributor {
  PsiJavaElementPattern componentNamePattern =
    psiElement().insideAnnotationParam(StandardPatterns.string().oneOf(SeamAnnotationConstants.COMPONENT_ANNOTATION));

  @Override
  public void fillCompletionVariants(@NotNull final CompletionParameters parameters, @NotNull final CompletionResultSet result) {
    final PsiElement position = parameters.getPosition();
    ApplicationManager.getApplication().runReadAction(() -> {
      if (componentNamePattern.accepts(position)) {
        result.stopHere();
        final PsiClass psiClass = PsiTreeUtil.getParentOfType(position, PsiClass.class);
        if (psiClass != null) {
          Project project = psiClass.getProject();

          final JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(project);
          SuggestedNameInfo info = codeStyleManager
            .suggestVariableName(VariableKind.PARAMETER, null, null, JavaPsiFacade.getElementFactory(project).createType(psiClass));

          for (String name : info.names) {
            result.addElement(LookupElementBuilder.create(name));
          }
        }
      }
    });

    //super.fillCompletionVariants(parameters, result);
  }
}
