/*
 * Copyright 2000-2006 JetBrains s.r.o.
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
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.codeInsight.GwtReferenceUtil;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.references.GwtToCssClassReference;
import com.intellij.gwt.uiBinder.UiBinderUtil;
import com.intellij.psi.JavaRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.XmlRecursiveElementVisitor;
import com.intellij.psi.css.StylesheetFile;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class GwtToCssClassReferencesInspection extends BaseGwtInspection {
  @Override
  public ProblemDescriptor[] checkFile(@NotNull PsiFile file, final @NotNull InspectionManager manager, final boolean isOnTheFly) {
    if (!shouldCheck(file)) return null;

    final List<ProblemDescriptor> problems = new SmartList<>();
    if (file instanceof PsiJavaFile) {
      file.accept(new JavaRecursiveElementWalkingVisitor() {
        @Override
        public void visitLiteralExpression(@NotNull PsiLiteralExpression expression) {
          checkReferences(expression, problems, manager, isOnTheFly);
        }

        @Override
        public void visitReferenceExpression(@NotNull PsiReferenceExpression expression) {
        }
      });
    }
    else if (file instanceof XmlFile && UiBinderUtil.isUiXmlFile((XmlFile)file)) {
      file.accept(new XmlRecursiveElementVisitor() {
        @Override
        public void visitXmlAttributeValue(@NotNull XmlAttributeValue value) {
          checkReferences(value, problems, manager, isOnTheFly);
        }
      });
    }
    if (problems.isEmpty()) {
      return null;
    }
    return problems.toArray(ProblemDescriptor.EMPTY_ARRAY);
  }

  private static void checkReferences(PsiElement element, List<ProblemDescriptor> problems, InspectionManager manager,
                                      boolean isOnTheFly) {
    for (PsiReference reference : element.getReferences()) {
      if (reference instanceof GwtToCssClassReference cssRef) {
        if (cssRef.multiResolve(false).length > 0) continue;

        String className = cssRef.getValue();
        List<LocalQuickFix> fixesList = new SmartList<>();
        if (isOnTheFly) {
          final GwtModule module = GwtReferenceUtil.findGwtModule(reference.getElement());
          if (module != null) {
            StylesheetFile file = GwtModulesManager.getInstance(element.getProject()).findPreferableCssFile(module);
            if (file != null && !className.contains(".")) {
              fixesList.add(new CreateCssClassLocalQuickFix(file, className));
            }
          }
          if (element instanceof XmlAttributeValue) {
            fixesList.add(new CreateLocalCssClassLocalQuickFix((XmlAttributeValue)element, className));
          }
        }
        LocalQuickFix[] fixes = fixesList.toArray(LocalQuickFix.EMPTY_ARRAY);
        problems.add(manager.createProblemDescriptor(element, reference.getRangeInElement(), GwtBundle.message("problem.description.unknown.css.class", className),
                                                     ProblemHighlightType.LIKE_UNKNOWN_SYMBOL, isOnTheFly, fixes));
      }
    }
  }
}
