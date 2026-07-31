package com.intellij.gwt.uiBinder.references;

import com.intellij.gwt.uiBinder.declarations.UiXmlDeclarationsManager;
import com.intellij.gwt.uiBinder.declarations.UiXmlVariableDeclaration;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiType;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class UiXmlVariableReference extends PsiReferenceBase<XmlAttributeValue> implements UiXmlElementReference {
  public UiXmlVariableReference(@NotNull XmlAttributeValue element, TextRange range) {
    super(element, range, true);
  }

  @Override
  public XmlTag resolve() {
    final UiXmlVariableDeclaration element = findDeclaration();
    return element != null ? element.getTag() : null;
  }

  public @Nullable UiXmlVariableDeclaration findDeclaration() {
    final PsiFile file = myElement.getContainingFile();
    if (file instanceof XmlFile) {
      return UiXmlDeclarationsManager.findDeclaration((XmlFile)file, getValue());
    }
    return null;
  }

  @Override
  public Object @NotNull [] getVariants() {
    final PsiFile file = myElement.getContainingFile();
    if (file instanceof XmlFile) {
      List<String> fields = new ArrayList<>();
      for (UiXmlVariableDeclaration element : UiXmlDeclarationsManager.getDeclarations((XmlFile)file)) {
        fields.add(element.getVariableName());
      }
      return ArrayUtilRt.toStringArray(fields);
    }
    return ArrayUtilRt.EMPTY_OBJECT_ARRAY;
  }

  @Override
  public PsiType resolveVariableType() {
    final UiXmlVariableDeclaration declaration = findDeclaration();
    return declaration != null ? declaration.resolveType() : null;
  }
}
