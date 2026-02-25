// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.directive;

import com.intellij.codeInsight.daemon.impl.analysis.encoding.XmlEncodingReference;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceService;
import com.intellij.psi.impl.source.xml.XmlAttributeValueImpl;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.lang.gsp.parsing.GspElementTypes;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.directive.GspDirectiveAttribute;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.directive.GspDirectiveAttributeValue;
import org.jetbrains.plugins.grails.references.common.ContentTypeReference;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GspDirectiveAttributeValueImpl extends XmlAttributeValueImpl implements GspDirectiveAttributeValue {

  public static final Pattern CHARSET_PATTERN = Pattern.compile("\\bcharset=([a-z\\-0-9]*)", Pattern.CASE_INSENSITIVE);
  private static final Pattern CONTENT_TYPE_PATTERN = Pattern.compile("\\s*([a-z\\-0-9/]+).*", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

  @Override
  public @NotNull IElementType getElementType() {
    return GspElementTypes.GSP_DIRECTIVE_ATTRIBUTE_VALUE;
  }

  @Override
  public PsiReference @NotNull [] getReferences(PsiReferenceService.@NotNull Hints hints) {
    PsiReference[] refs = super.getReferences(hints);
    if (refs.length > 0) return refs;

    GspDirectiveAttribute attribute = getContainingAttribute();
    if (attribute != null && "contentType".equals(attribute.getName())) {
      String value = getValue();

      List<PsiReference> result = new ArrayList<>();

      TextRange range = ElementManipulators.getValueTextRange(this);

      Matcher matcher = CHARSET_PATTERN.matcher(value);
      if (matcher.find()) {
        result.add(new XmlEncodingReference(this, matcher.group(1),
                                   TextRange.from(range.getStartOffset() + matcher.start(1), matcher.group(1).length()), 0));
      }

      matcher = CONTENT_TYPE_PATTERN.matcher(value);
      if (matcher.matches()) {
        result.add(new ContentTypeReference(this, TextRange.from(range.getStartOffset() + matcher.start(1), matcher.group(1).length()), true));
      }

      return result.toArray(PsiReference.EMPTY_ARRAY);
    }

    return PsiReference.EMPTY_ARRAY;
  }

  private @Nullable GspDirectiveAttribute getContainingAttribute() {
    PsiElement parent = getParent();
    if (parent instanceof GspDirectiveAttribute) {
      return (GspDirectiveAttribute) parent;
    }

    return null;
  }

  @Override
  public String toString() {
    return "GSP directive attribute value";
  }
}