// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails;

import com.intellij.codeInsight.TailType;
import com.intellij.codeInsight.TailTypes;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.TagNameReferenceCompletionProvider;
import com.intellij.codeInsight.lookup.AutoCompletionPolicy;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.TailTypeDecorator;
import com.intellij.lang.html.HTMLLanguage;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.fileType.GspFileType;
import org.jetbrains.plugins.grails.lang.gsp.GspLanguage;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GspOuterHtmlElement;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspFile;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.directive.GspDirective;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspGrailsTag;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspXmlRootTag;

import static com.intellij.patterns.XmlPatterns.not;
import static com.intellij.patterns.XmlPatterns.or;
import static com.intellij.patterns.XmlPatterns.psiElement;
import static com.intellij.patterns.XmlPatterns.psiFile;
import static com.intellij.patterns.XmlPatterns.string;
import static com.intellij.patterns.XmlPatterns.virtualFile;
import static com.intellij.patterns.XmlPatterns.xmlTag;

/**
 * @author Maxim.Medvedev
 */
public final class GspCompletionContributor extends CompletionContributor {

  public GspCompletionContributor() {
    extend(CompletionType.BASIC,
           psiElement(XmlTokenType.XML_NAME).withText(not(string().contains(":"))).withParent(
             or(
               xmlTag().inFile(
                 psiFile().withOriginalFile(
                   psiFile().withLanguage(HTMLLanguage.INSTANCE).withVirtualFile(
                     virtualFile().ofType(GspFileType.GSP_FILE_TYPE)))),
               psiElement(PsiErrorElement.class).inFile(
                 psiFile().withOriginalFile(
                   psiFile().withLanguage(HTMLLanguage.INSTANCE).withVirtualFile(
                     virtualFile().ofType(GspFileType.GSP_FILE_TYPE))))
             )
           ),
           new CompletionProvider<>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters,
                                           @NotNull ProcessingContext context,
                                           @NotNull CompletionResultSet result) {
               PsiElement element = parameters.getPosition();

               if (PsiImplUtil.isLeafElementOfType(element.getPrevSibling(), XmlTokenType.XML_END_TAG_START)) {
                 PsiElement gspElement =
                   element.getContainingFile().getViewProvider().findElementAt(parameters.getOffset(), GspLanguage.INSTANCE);
                 assert gspElement instanceof GspOuterHtmlElement;

                 GspGrailsTag tag = PsiTreeUtil.getParentOfType(gspElement, GspGrailsTag.class, true);
                 if (tag != null) {
                   LookupElementBuilder builder = LookupElementBuilder.create(tag.getName());
                   result.addElement(TailTypeDecorator.withTail(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE.applyPolicy(builder),
                                                                TailType.createSimpleTailType('>')));
                 }
               }
               else {
                 final GspFile gspFile = (GspFile)element.getContainingFile().getViewProvider().getPsi(GspLanguage.INSTANCE);
                 assert gspFile != null;

                 GspXmlRootTag rootTag = gspFile.getRootTag();
                 assert rootTag != null;
                 for (LookupElement e : TagNameReferenceCompletionProvider.getTagNameVariants(rootTag, "")) {
                   result.addElement(e);
                 }
               }
             }
           });

    extend(CompletionType.BASIC, psiElement().afterLeaf("<%@", "@{").inside(GspDirective.class),
           new CompletionProvider<>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters,
                                           @NotNull ProcessingContext context,
                                           @NotNull CompletionResultSet result) {
               for (String directiveName : ContainerUtil.ar("page", "taglib")) {
                 result.addElement(TailTypeDecorator.withTail(LookupElementBuilder.create(directiveName), TailTypes.spaceType()));
               }
             }
           });
  }

  @Override
  public void beforeCompletion(@NotNull CompletionInitializationContext context) {
    final PsiFile file = context.getFile();
    if (!(file instanceof GspFile)) {
      return;
    }

    final int offset = context.getEditor().getCaretModel().getOffset();
    final PsiElement element = file.findElementAt(offset);
    if (element == null) {
      return;
    }

    final IElementType elementType = element.getNode().getElementType();
    if (elementType == XmlTokenType.XML_END_TAG_START && element.getTextRange().getStartOffset() < offset ||
        elementType == XmlTokenType.XML_TAG_NAME) {
      context.setDummyIdentifier("");
    }
  }
}
