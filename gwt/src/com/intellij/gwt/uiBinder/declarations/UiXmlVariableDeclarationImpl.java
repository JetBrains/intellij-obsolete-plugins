package com.intellij.gwt.uiBinder.declarations;

import com.intellij.gwt.uiBinder.UiBinderUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiType;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UiXmlVariableDeclarationImpl implements UiXmlVariableDeclaration {
  protected final XmlTag myTag;
  protected final String myFieldName;
  protected final String myType;

  protected UiXmlVariableDeclarationImpl(@NotNull XmlTag tag, @Nullable String defaultFieldName) {
    this(tag, getFieldName(tag, defaultFieldName), tag.getAttributeValue(UiBinderUtil.UI_TYPE_ATTRIBUTE));
  }

  protected UiXmlVariableDeclarationImpl(@NotNull XmlTag tag, @Nullable String fieldName, @Nullable String type) {
    myTag = tag;
    myFieldName = fieldName;
    myType = type;
  }

  private static String getFieldName(@NotNull XmlTag tag, @Nullable String defaultFieldName) {
    final String fieldAttribute = tag.getAttributeValue(UiBinderUtil.UI_FIELD_ATTRIBUTE);
    return fieldAttribute != null ? fieldAttribute : defaultFieldName;
  }

  @Override
  public @Nullable String getVariableName() {
    return myFieldName;
  }

  @Override
  public @Nullable String getType() {
    return myType;
  }

  @Override
  public @Nullable PsiType resolveType() {
    if (myType == null) return null;
    return JavaPsiFacade.getInstance(myTag.getProject()).getElementFactory().createTypeByFQClassName(myType, myTag.getResolveScope());
  }

  @Override
  public @NotNull XmlTag getTag() {
    return myTag;
  }
}
