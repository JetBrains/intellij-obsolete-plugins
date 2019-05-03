/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.generate;

import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.impl.TemplateSettings;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomManager;
import com.intellij.util.xml.actions.generate.DefaultGenerateElementProvider;
import org.jetbrains.annotations.NonNls;

/**
 * @author Dmitry Avdeev
 */
public class GenerateMappingProvider<T extends DomElement> extends DefaultGenerateElementProvider<T> {

  private final Class<? extends DomElement>[] myPossibleParents;
  private final String myMappingId;

  public GenerateMappingProvider(final String name, final Class<T> childElementClass,
                                 @NonNls final String mappingId, final Class<? extends DomElement>... possibleParents) {
    super(name, childElementClass);
    myPossibleParents = possibleParents;
    myMappingId = mappingId;
  }

  @Override
  public T generate(final Project project, final Editor editor, final PsiFile file) {
    final T t = super.generate(project, editor, file);
    if (t != null) {
      final Template template = TemplateSettings.getInstance().getTemplateById(myMappingId);
      if (template != null) {
        final DomElement copy = t.createStableCopy();
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());
        final XmlTag tag = copy.getXmlTag();
        assert tag != null;
        editor.getCaretModel().moveToOffset(tag.getTextRange().getStartOffset());
        copy.undefine();

        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());
        TemplateManager.getInstance(project).startTemplate(editor, template);
      }
    }
    return t;
  }

  @Override
  public void navigate(final DomElement element) {}

  @Override
  public DomElement getParentDomElement(final Project project, final Editor editor, final PsiFile file) {
    if (file instanceof XmlFile) {
      final int offset = editor.getCaretModel().getOffset();
      final PsiElement element = file.findElementAt(offset);
      if (element == null) return null;

      final XmlTag tag = PsiTreeUtil.getParentOfType(element, XmlTag.class);
      if (tag != null) {
        DomElement dom = DomManager.getDomManager(project).getDomElement(tag);
        while (dom != null) {
          for (final Class<? extends DomElement> possibleParent : myPossibleParents) {
            if (possibleParent.isInstance(dom)) {
              return dom;
            }
          }
          dom = dom.getParent();
        }
      }
    }
    return null;
  }
}
