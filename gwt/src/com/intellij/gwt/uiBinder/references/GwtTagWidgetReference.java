package com.intellij.gwt.uiBinder.references;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.xml.TagNameReference;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

import static com.intellij.gwt.uiBinder.UiBinderUtil.URN_IMPORT_PREFIX;
import static com.intellij.gwt.uiBinder.references.UiXmlTagNameRefactoringUtil.handleMove;
import static com.intellij.openapi.util.text.StringUtil.getPackageName;
import static com.intellij.openapi.util.text.StringUtil.trimStart;

public class GwtTagWidgetReference extends TagNameReference {

  public GwtTagWidgetReference(ASTNode nameElement, boolean startTagFlag) {
    super(nameElement, startTagFlag);
  }

  @Override
  public PsiElement bindToElement(@NotNull PsiElement element) {
    if (element instanceof PsiClass psiClass) {
      XmlTag xmlTag = getTagElement();
      String newQualifiedClassName = psiClass.getQualifiedName();
      if (xmlTag != null && newQualifiedClassName != null) {
        String oldNamespacePackage = trimStart(xmlTag.getNamespace(), URN_IMPORT_PREFIX);
        String oldTagName = trimStart(xmlTag.getName(), xmlTag.getNamespacePrefix() + ":");

        String oldQualifiedClassName = oldNamespacePackage + "." + oldTagName;

        String oldPackage = getPackageName(oldQualifiedClassName);
        String newPackage = getPackageName(newQualifiedClassName);

        if (newPackage.equals(oldPackage)) { // regular rename, leave old namespace
          return handleElementRename(trimStart(newQualifiedClassName, oldNamespacePackage + "."));
        }
        else {
          return handleElementRename(handleMove(xmlTag, newPackage, psiClass.getName()));
        }
      }
    }
    return super.bindToElement(element);
  }

  @Override
  public boolean isSoft() {
    return true;
  }
}
