package com.intellij.jboss.bpmn.jpdl.rename;

import com.intellij.jboss.bpmn.jpdl.model.xml.JpdlNamedActivity;
import com.intellij.patterns.DomPatterns;
import com.intellij.patterns.ElementPattern;
import com.intellij.pom.PomTarget;
import com.intellij.pom.PomTargetPsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.refactoring.rename.RenameInputValidator;
import com.intellij.util.ProcessingContext;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomManager;
import com.intellij.util.xml.DomTarget;

public class JpdlNamedActivityInputValidator implements RenameInputValidator {

  @Override
  public ElementPattern<? extends PsiElement> getPattern() {
    return DomPatterns.domTargetElement(DomPatterns.domElement(JpdlNamedActivity.class));
  }

  @Override
  public boolean isInputValid(final String newName, final PsiElement element, final ProcessingContext context) {
    return (getDomElement(element) instanceof JpdlNamedActivity) && !(newName.contains("\""));
  }

  private DomElement getDomElement(PsiElement element) {
    if (element instanceof XmlTag) {
      return DomManager.getDomManager(element.getProject()).getDomElement((XmlTag)element);
    }
    else if (element instanceof PomTargetPsiElement) {
      final PomTarget target = ((PomTargetPsiElement)element).getTarget();
      if (target instanceof DomTarget) {
        return ((DomTarget)target).getDomElement();
      }
    }
    return null;
  }
}