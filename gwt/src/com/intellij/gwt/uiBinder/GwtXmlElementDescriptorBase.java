package com.intellij.gwt.uiBinder;

import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlElementDescriptorAwareAboutChildren;
import com.intellij.xml.XmlElementsGroup;
import com.intellij.xml.XmlNSDescriptor;
import org.jetbrains.annotations.NotNull;

public abstract class GwtXmlElementDescriptorBase implements XmlElementDescriptor, XmlElementDescriptorAwareAboutChildren {
  protected final GwtUiComponentsNSDescriptor myXmlNSDescriptor;
  protected final String myNamespacePrefix;

  public GwtXmlElementDescriptorBase(final @NotNull GwtUiComponentsNSDescriptor xmlNSDescriptor,
                                     @NotNull String namespacePrefix) {
    myXmlNSDescriptor = xmlNSDescriptor;
    myNamespacePrefix = namespacePrefix;
  }

  @Override
  public String getName() {
    return getDefaultName();
  }

  @Override
  public String getQualifiedName() {
    return getDefaultName();
  }

  @Override
  public boolean allowElementsFromNamespace(String namespace, XmlTag context) {
    return true;
  }

  @Override
  public XmlNSDescriptor getNSDescriptor() {
    return myXmlNSDescriptor;
  }

  @Override
  public XmlElementsGroup getTopGroup() {
    return null;
  }

  @Override
  public int getContentType() {
    return CONTENT_TYPE_ANY;
  }

  @Override
  public String getDefaultValue() {
    return null;
  }

  @Override
  public XmlAttributeDescriptor getAttributeDescriptor(XmlAttribute attribute) {
    return getAttributeDescriptor(attribute.getName(), attribute.getParent());
  }

  @Override
  public String getName(PsiElement context) {
    return getDefaultName();
  }

  @Override
  public void init(PsiElement element) {
  }
}
