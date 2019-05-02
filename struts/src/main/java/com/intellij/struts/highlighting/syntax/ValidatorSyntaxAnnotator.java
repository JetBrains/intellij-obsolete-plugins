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

package com.intellij.struts.highlighting.syntax;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.util.TextRange;
import com.intellij.struts.dom.validator.FormValidation;
import com.intellij.struts.dom.validator.Var;
import com.intellij.util.xml.GenericDomValue;

/**
 * Provides additional syntax highlighting for Validator config files.
 *
 * @author Yann C�bron
 */
public class ValidatorSyntaxAnnotator extends DomAnnotatorComponentBase<FormValidation> {

  public ValidatorSyntaxAnnotator() {
    super(FormValidation.class);
  }

  @Override
  protected DomAnnotatorVisitor buildVisitor(final AnnotationHolder holder) {
    return new DomAnnotatorVisitor(holder) {

      public void visitVar(final Var var) {
        final GenericDomValue<String> varValue = var.getVarValue();
        final String value = varValue.getStringValue();
        if (value == null) {
          return;
        }

        // highlight missing braces when using reference to constant
        final boolean expressionStart = value.startsWith("${");
        final boolean expressionEnd = value.endsWith("}");
        if (expressionStart && !expressionEnd ||
            !expressionStart && expressionEnd) {
          final TextRange textRange = varValue.ensureTagExists().getValue().getTextRange();
          final int existingBraceOffset = expressionStart ? textRange.getStartOffset() + 1 : textRange.getEndOffset() - 1;
          holder.createErrorAnnotation(new TextRange(existingBraceOffset, existingBraceOffset + 1),
                                       "Unmatched brace")
            .setTextAttributes(CodeInsightColors.UNMATCHED_BRACE_ATTRIBUTES);
        }
      }

    };
  }
}
