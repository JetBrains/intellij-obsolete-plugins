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

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.daemon.impl.quickfix.ReferenceNameExpression;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateBuilderFactory;
import com.intellij.codeInsight.template.TemplateBuilderImpl;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.references.GwtToHtmlTagReference;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.openapi.util.text.StringUtil.unquoteString;
import static com.intellij.util.containers.ContainerUtil.findInstance;
import static java.util.Arrays.stream;

public final class GwtToHtmlTagReferencesInspection extends BaseGwtInspection {
  @Override
  public @NotNull PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
    if (!hasGwtFacets(holder.getManager().getProject())) {
      return super.buildVisitor(holder, isOnTheFly);
    }
    return new JavaElementVisitor() {
      @Override
      public void visitLiteralExpression(@NotNull PsiLiteralExpression expression) {
        final PsiReference[] references = expression.getReferences();
        for (PsiReference reference : references) {
          if (reference instanceof GwtToHtmlTagReference && reference.resolve() == null) {
            holder.registerProblem(expression, GwtBundle.message("problem.description.html.tag.with.id.0.is.not.found", expression.getValue()),
                                   ProblemHighlightType.LIKE_UNKNOWN_SYMBOL, new GwtToHtmlTagRenameReferenceQuickFix(expression));
          }
        }
      }
    };
  }

  private static class GwtToHtmlTagRenameReferenceQuickFix extends LocalQuickFixAndIntentionActionOnPsiElement {

    GwtToHtmlTagRenameReferenceQuickFix(@NotNull PsiLiteralExpression element) {
      super(element);
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @Nullable Editor editor,
                       @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
      if (editor == null) return;

      TemplateBuilderImpl builder = (TemplateBuilderImpl)TemplateBuilderFactory.getInstance().createTemplateBuilder(startElement);
      TextRange textRange = TextRange.from(1, startElement.getTextLength() - 2); // ignore quotes

      GwtToHtmlTagReference reference = findInstance(startElement.getReferences(), GwtToHtmlTagReference.class);
      if (reference == null) return;

      LookupElement[] items = stream(reference.getVariants()).map(LookupElementBuilder::create).toArray(LookupElement[]::new);
      builder.replaceElement(startElement, textRange, new ReferenceNameExpression(items, unquoteString(startElement.getText())));

      editor.getCaretModel().moveToOffset(startElement.getTextOffset());

      Template template = builder.buildInlineTemplate();
      TemplateManager.getInstance(project).startTemplate(editor, template);
    }

    @Override
    public @NotNull String getText() {
      return GwtBundle.message("quick.fix.name.rename.reference");
    }

    @Override
    public @Nls @NotNull String getFamilyName() {
      return CodeInsightBundle.message("rename.element.family");
    }
  }
}
