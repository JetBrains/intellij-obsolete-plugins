/*
 * Copyright 2000-2007 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.gwt.inspections;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.jsinject.GwtClassMemberReference;
import com.intellij.gwt.jsinject.JSGwtReferenceExpressionImpl;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.psi.util.PsiFormatUtilBase;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class GwtJavaScriptReferencesInspection extends BaseJavaFromJSReferenceInspection {
  @Override
  protected void checkReferenceExpression(JSGwtReferenceExpressionImpl element,
                                          List<ProblemDescriptor> problems,
                                          InspectionManager manager,
                                          boolean isOnTheFly) {
    PsiReference[] references = element.getReferences();
    for (PsiReference reference : references) {
      if (reference.resolve() == null) {
        if (reference instanceof GwtClassMemberReference gwtReference) {
          PsiClass psiClass = gwtReference.resolveQualifier();
          if (psiClass != null) {
            ResolveResult[] results = gwtReference.multiResolve(false);
            String message;
            if (results.length > 1) {
              String method1String = formatMethod((PsiMethod)results[0].getElement());
              String method2String = formatMethod((PsiMethod)results[1].getElement());
              message = GwtBundle.message("problem.description.ambiguous.wildcard.match",
                                          method1String, method2String, psiClass.getQualifiedName(), reference.getCanonicalText());
            }
            else {
              message = GwtBundle.message("problem.description.cannot.resolve.symbol.0.in.1", reference.getCanonicalText(),
                                          psiClass.getQualifiedName());
            }
            problems.add(manager.createProblemDescriptor(element, reference.getRangeInElement(), message,
                                                         ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly));
          }
        }
        else {
          String message = GwtBundle.message("problem.description.cannot.resolve.0", reference.getCanonicalText());
          problems.add(manager.createProblemDescriptor(element, reference.getRangeInElement(), message,
                                                       ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly));
        }
      }
    }
  }

  private static @NotNull String formatMethod(PsiMethod method1) {
    return PsiFormatUtil.formatMethod(method1, PsiSubstitutor.EMPTY, PsiFormatUtilBase.SHOW_NAME | PsiFormatUtilBase.SHOW_PARAMETERS,
                                      PsiFormatUtilBase.SHOW_TYPE);
  }
}
