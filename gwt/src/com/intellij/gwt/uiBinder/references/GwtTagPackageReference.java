package com.intellij.gwt.uiBinder.references;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PsiPackageReference;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import static com.intellij.gwt.uiBinder.references.UiXmlTagNameRefactoringUtil.handleMove;
import static com.intellij.openapi.util.text.StringUtil.trimEnd;

class GwtTagPackageReference extends PsiPackageReference {

  private final TextRange myRangeInName;

  GwtTagPackageReference(GwtTagPackageReferenceSet referenceSet, TextRange range, int index) {
    super(referenceSet, range, index);
    myRangeInName = TextRange.create(range.getStartOffset() - 1, range.getEndOffset() - 1); // ignore "<" character
  }

  @Override
  public PsiElement handleElementRename(@NotNull String newElementName) {
    XmlTag xmlTag = getElement();
    return xmlTag.setName(myRangeInName.replace(xmlTag.getName(), newElementName));
  }

  @Override
  public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
    if (!(element instanceof PsiPackage psiPackage)) {
      throw new IncorrectOperationException("Cannot bind to " + element);
    }

    XmlTag xmlTag = getElement();
    String xmlTagName = xmlTag.getName();
    String entityName = xmlTagName.substring(myRangeInName.getStartOffset());

    String newQualifiedClassName = psiPackage.getQualifiedName() + xmlTagName.substring(myRangeInName.getEndOffset());
    String newPackage = trimEnd(newQualifiedClassName, "." + entityName);

    return xmlTag.setName(handleMove(xmlTag, newPackage, entityName));
  }

  @Override
  public @NotNull XmlTag getElement() {
    return (XmlTag)super.getElement();
  }
}
