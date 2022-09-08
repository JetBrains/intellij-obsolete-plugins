package com.intellij.seam.pageflow;

import com.intellij.patterns.DomPatterns;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.refactoring.rename.RenameInputValidator;
import com.intellij.seam.pageflow.model.xml.pageflow.PageflowNamedElement;
import com.intellij.util.ProcessingContext;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomManager;

public class PageflowRenameInputValidator implements RenameInputValidator {
  @Override
  public ElementPattern<? extends PsiElement> getPattern() {
    return DomPatterns.domTargetElement(DomPatterns.domElement(PageflowNamedElement.class));
  }

  @Override
  public boolean isInputValid(final String newName, final PsiElement element, final ProcessingContext context) {
    final DomElement domElement = DomManager.getDomManager(element.getProject()).getDomElement((XmlTag)element);

    return (domElement instanceof PageflowNamedElement) && !(newName.contains("\""));
  }
}
