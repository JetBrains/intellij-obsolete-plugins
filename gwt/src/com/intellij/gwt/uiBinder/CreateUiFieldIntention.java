package com.intellij.gwt.uiBinder;

import com.intellij.gwt.GwtBundle;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiType;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction;

public final class CreateUiFieldIntention extends GwtUiXmlUiFieldIntentionBase {

  private static final Logger LOG = Logger.getInstance(CreateUiFieldIntention.class);

  @Override
  protected void invokeInternal(Project project, XmlAttributeValue attributeValue, PsiClass psiClass) {
    runWriteCommandAction(project, () -> {
      final PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
      final XmlTag tag = PsiTreeUtil.getParentOfType(attributeValue, XmlTag.class);
      if (tag == null) return;

      String className = UiBinderUtil.getComponentClassName(tag);
      PsiType type = elementFactory.createTypeByFQClassName(className, psiClass.getResolveScope());
      final PsiField field = elementFactory.createField(attributeValue.getValue(), type);
      final PsiModifierList modifierList = field.getModifierList();
      LOG.assertTrue(modifierList != null, field.getClass());
      modifierList.setModifierProperty(PsiModifier.PRIVATE, false);
      modifierList.addAnnotation(UiBinderUtil.UI_FIELD_ANNOTATION);

      PsiField lastField = null;
      PsiField lastUiField = null;
      for (PsiField psiField : psiClass.getFields()) {
        if (UiBinderUtil.isUiField(psiField)) {
          lastUiField = psiField;
        }
        lastField = psiField;
      }
      if (lastUiField != null) {
        lastField = lastUiField;
      }

      final PsiElement added = lastField != null ? psiClass.addAfter(field, lastField) : psiClass.add(field);

      JavaCodeStyleManager.getInstance(project).shortenClassReferences(added);
      CodeStyleManager.getInstance(project).reformat(added);
    });
  }

  @Override
  protected PsiClass filterClass(List<PsiClass> classes, String fieldName) {
    for (PsiClass psiClass : classes) {
      if (psiClass.findFieldByName(fieldName, true) == null) {
        return psiClass;
      }
    }
    return null;
  }

  @Override
  public @NotNull String getText() {
    return GwtBundle.message("intention.text.create.uifield.for.tag");
  }
}
