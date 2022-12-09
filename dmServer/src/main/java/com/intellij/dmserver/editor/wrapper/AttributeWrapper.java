package com.intellij.dmserver.editor.wrapper;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.lang.manifest.psi.HeaderValuePart;
import org.osmorc.manifest.lang.psi.AssignmentExpression;

public class AttributeWrapper {

  private final String myName;
  private final String myValue;

  public AttributeWrapper(String attributeName, String attributeValue) {
    myName = attributeName;
    myValue = attributeValue;
  }

  @Nullable
  public static AttributeWrapper create(AssignmentExpression attribute) {
    HeaderValuePart nameValuePart = PsiTreeUtil.getChildOfType(attribute, HeaderValuePart.class);
    if (nameValuePart == null) {
      return null;
    }
    HeaderValuePart valueValuePart = PsiTreeUtil.getNextSiblingOfType(nameValuePart, HeaderValuePart.class);
    return new AttributeWrapper(nameValuePart.getUnwrappedText(),
                                valueValuePart == null ? null : trimValue(valueValuePart.getUnwrappedText()));
  }

  private static String trimValue(@NotNull String attributeValue) {
    attributeValue = StringUtil.trimStart(attributeValue, "\"");
    attributeValue = StringUtil.trimEnd(attributeValue, "\"");
    return attributeValue;
  }

  public String getName() {
    return myName;
  }

  @Nullable
  public String getValue() {
    return myValue;
  }
}
