package com.intellij.gwt.uiBinder.references;

import com.intellij.gwt.references.GwtToCssClassReferenceProvider;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class QualifiedUiXmlReferenceProvider extends PsiReferenceProvider {
  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    if (element instanceof XmlAttributeValue attributeValue) {
      final String attributeText = attributeValue.getValue();
      final List<TextRange> ranges = GwtToCssClassReferenceProvider.getWordRanges(attributeText, 0);
      List<PsiReference> references = new ArrayList<>();
      for (final TextRange wordRange : ranges) {
        final String word = wordRange.substring(attributeText);
        if (word.startsWith("{")) {
          int end = word.indexOf('}');
          TextRange range = TextRange.from(wordRange.getStartOffset() + 1, end != -1 ? end - 1 : word.length() - 1);
          String refText = range.substring(attributeText);
          collectReferences(attributeValue, range, refText, references);
        }
      }
      return references.toArray(PsiReference.EMPTY_ARRAY);

    }
    return PsiReference.EMPTY_ARRAY;
  }

  private static void collectReferences(XmlAttributeValue attributeValue, TextRange range, String refText, List<PsiReference> references) {
    UiXmlElementReference ref = null;
    int start = 0;
    while (true) {
      int dot = refText.indexOf('.', start);
      int end = dot == -1 ? refText.length() : dot;
      TextRange refRange = new TextRange(start, end).shiftRight(range.getStartOffset() + 1);
      if (ref == null) {
        ref = new UiXmlVariableReference(attributeValue, refRange);
      }
      else {
        ref = new QualifiedUiXmlReference(ref, attributeValue, refRange);
      }
      references.add(ref);
      if (dot == -1) {
        break;
      }
      start = dot + 1;
    }
  }
}
