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

package com.intellij.gwt.references;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GwtToCssClassReferenceProvider extends PsiReferenceProvider {
  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, final @NotNull ProcessingContext context) {
    if (element instanceof PsiLiteralExpression literalExpression) {
      final Object value = literalExpression.getValue();
      if (value instanceof String text) {
        List<TextRange> ranges = getWordRanges(text, 1);
        final PsiReference[] references = new PsiReference[ranges.size()];
        for (int i = 0; i < ranges.size(); i++) {
          final TextRange rangeInElement = ranges.get(i);
          references[i] = new GwtToCssClassReference<>(literalExpression, rangeInElement);
        }
        return references;
      }
    }

    if (element instanceof XmlAttributeValue attributeValue) {
      final String attributeText = attributeValue.getValue();
      final List<TextRange> ranges = getWordRanges(attributeText, 1);
      List<PsiReference> references = new ArrayList<>();
      for (final TextRange range : ranges) {
        final String refText = range.shiftRight(-1).substring(attributeText);
        if (!refText.startsWith("{")) {
          references.add(new GwtToCssClassReference<>(attributeValue, range));
        }
      }
      return references.toArray(PsiReference.EMPTY_ARRAY);
    }
    return PsiReference.EMPTY_ARRAY;
  }

  public static List<TextRange> getWordRanges(String text, int shift) {
    List<TextRange> ranges = new ArrayList<>();
    int start = 0;
    while (true) {
      int end = text.indexOf(' ', start);
      if (end == -1) {
        if (start < text.length()) {
          ranges.add(new TextRange(shift + start, shift + text.length()));
        }
        break;
      }
      if (start < end) {
        ranges.add(new TextRange(shift + start, shift + end));
      }
      start = end + 1;
    }
    return ranges;
  }

}
