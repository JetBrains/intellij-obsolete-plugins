package com.intellij.gwt.uiBinder.declarations;

import com.intellij.gwt.uiBinder.UiBinderUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class UiImportVariable implements UiXmlVariableDeclaration {
  private final XmlTag myTag;
  private final String myClassName;
  private final String myFieldName;

  UiImportVariable(@NotNull XmlTag tag, @NotNull String className, @NotNull String fieldName) {
    myTag = tag;
    myClassName = className;
    myFieldName = fieldName;
  }

  static @NotNull List<UiImportVariable> createElements(@NotNull XmlTag tag) {
    String qualifiedFieldName = tag.getAttributeValue(UiBinderUtil.UI_FIELD_ATTRIBUTE);
    if (qualifiedFieldName == null) return Collections.emptyList();
    String className = StringUtil.getPackageName(qualifiedFieldName);
    if (className.isEmpty()) return Collections.emptyList();

    String fieldName = StringUtil.getShortName(qualifiedFieldName);
    if (!fieldName.equals("*")) {
      return Collections.singletonList(new UiImportVariable(tag, className, fieldName));
    }

    PsiClass psiClass = JavaPsiFacade.getInstance(tag.getProject()).findClass(className, tag.getResolveScope());
    if (psiClass == null) return Collections.emptyList();
    List<UiImportVariable> variables = new ArrayList<>();
    for (PsiField field : psiClass.getFields()) {
      if (field.hasModifierProperty(PsiModifier.STATIC) && field.hasModifierProperty(PsiModifier.PUBLIC)) {
        variables.add(new UiImportVariable(tag, className, field.getName()));
      }
    }

    return variables;
  }

  @Override
  public @Nullable String getVariableName() {
    return myFieldName;
  }

  @Override
  public @Nullable String getType() {
    PsiClass psiClass = JavaPsiFacade.getInstance(myTag.getProject()).findClass(myClassName, myTag.getResolveScope());
    if (psiClass == null) return null;

    PsiField field = psiClass.findFieldByName(myFieldName, false);
    return field != null ? field.getType().getCanonicalText() : null;
  }

  @Override
  public @Nullable PsiType resolveType() {
    PsiClass psiClass = JavaPsiFacade.getInstance(myTag.getProject()).findClass(myClassName, myTag.getResolveScope());
    if (psiClass == null) return null;

    PsiField field = psiClass.findFieldByName(myFieldName, false);
    return field != null ? field.getType() : null;
  }

  @Override
  public @NotNull XmlTag getTag() {
    return myTag;
  }
}
