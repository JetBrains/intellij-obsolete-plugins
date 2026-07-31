package com.intellij.gwt.uiBinder;

import com.intellij.javaee.model.annotations.AnnotationModelUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.impl.schema.AnyXmlElementDescriptor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class GwtUiChildElementDescriptor extends GwtXmlElementDescriptorBase {
  public static final GwtUiChildElementDescriptor[] EMPTY_ARRAY = new GwtUiChildElementDescriptor[0];
  private final PsiMethod myMethod;
  protected String myTagName;
  private final Map<String, XmlAttributeDescriptor> myAttributes;

  public GwtUiChildElementDescriptor(@NotNull GwtUiComponentsNSDescriptor xmlNSDescriptor,
                                     @NotNull String namespacePrefix,
                                     @NotNull PsiMethod method, PsiAnnotation annotation) {
    super(xmlNSDescriptor, namespacePrefix);
    myMethod = method;
    //final Integer limit = AnnotationModelUtil.getObjectValue(annotation, "limit", Integer.class).getValue();
    myTagName = AnnotationModelUtil.getObjectValue(annotation, "tagname", String.class).getStringValue();
    if (StringUtil.isEmpty(myTagName)) {
      myTagName = StringUtil.toLowerCase(StringUtil.trimStart(method.getName(), "add"));
    }

    myAttributes = new LinkedHashMap<>();
    PsiParameter[] parameters = myMethod.getParameterList().getParameters();
    for (int i = 1; i < parameters.length; i++) {
      PsiParameter parameter = parameters[i];
      myAttributes.put(parameter.getName(), new GwtUiParameterAttributeDescriptor(parameter, false));
    }
  }

  public String getTagName() {
    return myTagName;
  }

  @Override
  public String getDefaultName() {
    return myNamespacePrefix + ":" + myTagName;
  }

  @Override
  public PsiElement getDeclaration() {
    return myMethod;
  }

  @Override
  public XmlElementDescriptor[] getElementsDescriptors(XmlTag context) {
    return EMPTY_ARRAY;
  }

  @Override
  public XmlElementDescriptor getElementDescriptor(XmlTag childTag, XmlTag contextTag) {
    return new AnyXmlElementDescriptor(this, myXmlNSDescriptor);
  }

  @Override
  public XmlAttributeDescriptor[] getAttributesDescriptors(@Nullable XmlTag context) {
    return myAttributes.values().toArray(XmlAttributeDescriptor.EMPTY);
  }

  @Override
  public XmlAttributeDescriptor getAttributeDescriptor(@NonNls String attributeName, @Nullable XmlTag context) {
    return myAttributes.get(attributeName);
  }
}
