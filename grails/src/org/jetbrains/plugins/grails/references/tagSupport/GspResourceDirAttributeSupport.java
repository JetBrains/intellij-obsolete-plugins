// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.tagSupport;

import com.intellij.openapi.paths.PathReference;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.common.GroovyGspTagWrapper;
import org.jetbrains.plugins.grails.references.common.GspTagWrapper;
import org.jetbrains.plugins.grails.references.common.ResourceDirAttributeFileReferenceSet;
import org.jetbrains.plugins.groovy.extensions.GroovyNamedArgumentReferenceProvider;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.util.GrNamedArgumentsOwner;

public class GspResourceDirAttributeSupport extends TagAttributeReferenceProvider implements GroovyNamedArgumentReferenceProvider {

  public static final String[] TAGS = {"resource", "createLinkTo"};

  public GspResourceDirAttributeSupport() {
    super("dir", "g", TAGS);
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                         @NotNull String text,
                                                         int offset,
                                                         @NotNull GspTagWrapper gspTagWrapper) {
    return createReferences(element, gspTagWrapper);
  }

  public static PsiReference[] createReferences(@NotNull PsiElement element, final @NotNull GspTagWrapper gspTagWrapper) {
    final TextRange range = ElementManipulators.getValueTextRange(element);
    int offset = range.getStartOffset();
    String text = range.substring(element.getText());

    String trimedUrl = PathReference.trimPath(text);

    if (trimedUrl.trim().isEmpty()) return PsiReference.EMPTY_ARRAY;

    final FileReferenceSet set = new ResourceDirAttributeFileReferenceSet(trimedUrl, element, offset, null, true, false) {

      @Override
      protected PsiElement getPluginElement() {
        return gspTagWrapper.getAttributeValue("plugin");
      }

      @Override
      protected PsiElement getContextPathElement() {
        return gspTagWrapper.getAttributeValue("contextPath");
      }
    };

    return set.getAllReferences();
  }

  @Override
  public PsiReference[] createRef(@NotNull PsiElement element,
                                  @NotNull GrNamedArgument namedArgument,
                                  @NotNull GroovyResolveResult resolveResult,
                                  @NotNull ProcessingContext context) {
    return createReferences(element, new GroovyGspTagWrapper((GrNamedArgumentsOwner)namedArgument.getParent(), null));
  }
}
