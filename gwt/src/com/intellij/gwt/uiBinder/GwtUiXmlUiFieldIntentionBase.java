package com.intellij.gwt.uiBinder;

import com.intellij.codeInsight.intention.AbstractIntentionAction;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.uiBinder.mapping.UiBinderMappingService;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNameHelper;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.intellij.gwt.uiBinder.UiBinderUtil.UI_FIELD_ATTRIBUTE;
import static com.intellij.psi.util.PsiUtil.getLanguageLevel;

public abstract class GwtUiXmlUiFieldIntentionBase extends AbstractIntentionAction {

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile psiFile) throws IncorrectOperationException {
    Pair<XmlAttributeValue, PsiClass> pair = findAttribute(psiFile, editor);
    if (pair == null) return;

    PsiClass psiClass = pair.getSecond();
    if (!CommonRefactoringUtil.checkReadOnlyStatus(project, psiClass)) {
      return;
    }

    invokeInternal(project, pair.getFirst(), psiClass);
  }

  protected abstract void invokeInternal(Project project, XmlAttributeValue attributeValue, PsiClass psiClass);

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile psiFile) {
    return findAttribute(psiFile, editor) != null;
  }

  protected final @Nullable Pair<XmlAttributeValue, PsiClass> findAttribute(PsiFile file, Editor editor) {
    if (!(file instanceof XmlFile)) return null;

    GwtFacet gwtFacet = GwtFacet.findFacetByPsiElement(file);
    if (gwtFacet == null) return null;

    PsiElement element = file.findElementAt(editor.getCaretModel().getOffset());
    XmlAttributeValue attributeValue = PsiTreeUtil.getParentOfType(element, XmlAttributeValue.class);
    if (attributeValue == null) return null;

    XmlAttribute attribute = PsiTreeUtil.getParentOfType(attributeValue, XmlAttribute.class);
    if (attribute == null || !attribute.getLocalName().equals(UI_FIELD_ATTRIBUTE) || !UiBinderUtil.hasUiBinderNamespace(attribute)) {
      return null;
    }

    String fieldName = attributeValue.getValue();
    List<PsiClass> classes = UiBinderMappingService.getInstance(gwtFacet.getModule()).getBoundClasses(file);
    PsiClass psiClass = filterClass(classes, fieldName);

    if (psiClass != null && PsiNameHelper.getInstance(attributeValue.getProject()).isIdentifier(fieldName, getLanguageLevel(psiClass))) {
      return Pair.create(attributeValue, psiClass);
    }

    return null;
  }

  protected abstract PsiClass filterClass(List<PsiClass> classes, String fieldName);
}
