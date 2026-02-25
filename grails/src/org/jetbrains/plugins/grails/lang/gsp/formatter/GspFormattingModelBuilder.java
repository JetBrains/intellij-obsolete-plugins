// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.formatter;

import com.intellij.formatting.FormattingContext;
import com.intellij.formatting.FormattingDocumentModel;
import com.intellij.formatting.FormattingModel;
import com.intellij.formatting.FormattingModelBuilder;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.formatter.FormattingDocumentModelImpl;
import com.intellij.psi.formatter.PsiBasedFormattingModel;
import com.intellij.psi.formatter.xml.HtmlPolicy;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.impl.source.tree.TreeUtil;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspTag;
import org.jetbrains.plugins.grails.lang.gsp.psi.html.api.GspLikeFile;

public final class GspFormattingModelBuilder implements FormattingModelBuilder {
  @Override
  public @NotNull FormattingModel createModel(@NotNull FormattingContext formattingContext) {
    ASTNode root = TreeUtil.getFileElement((TreeElement)SourceTreeToPsiMap.psiElementToTree(formattingContext.getPsiElement()));
    PsiFile containingFile = formattingContext.getContainingFile();
    if (containingFile instanceof GspLikeFile) {
      containingFile = ((GspLikeFile)containingFile).getGspLanguageRoot();
      root = containingFile.getNode();
    }
    final FormattingDocumentModelImpl documentModel = FormattingDocumentModelImpl.createOn(containingFile);
    return new PsiBasedFormattingModel(containingFile,
                                       new GspBlock(root, null, null,
                                                    new GspPolicy(formattingContext.getCodeStyleSettings(), documentModel), null, null),
                                       documentModel);
  }

  private static class GspPolicy extends HtmlPolicy {

    GspPolicy(final CodeStyleSettings settings, final FormattingDocumentModel documentModel) {
      super(settings, documentModel);
    }

    @Override
    protected boolean isInlineTag(XmlTag tag) {
      return tag instanceof GspTag || super.isInlineTag(tag);
    }
  }
}
