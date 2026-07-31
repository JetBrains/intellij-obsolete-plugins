package com.intellij.gwt.uiBinder;

import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlAttributeDescriptorsProvider;
import com.intellij.xml.impl.schema.AnyXmlAttributeDescriptor;
import com.intellij.xml.util.XmlUtil;

public final class GwtHtmlAttributeDescriptorProvider implements XmlAttributeDescriptorsProvider {
  @Override
  public XmlAttributeDescriptor[] getAttributeDescriptors(XmlTag context) {
    if (!isInUiXmlFile(context)) return XmlAttributeDescriptor.EMPTY;

    final String prefix = context.getPrefixByNamespace(UiBinderUtil.UI_BINDER_NAMESPACE);
    if (prefix != null) {
      return new XmlAttributeDescriptor[]{new AnyXmlAttributeDescriptor(prefix + ":" + UiBinderUtil.UI_FIELD_ATTRIBUTE)};
    }
    return XmlAttributeDescriptor.EMPTY;
  }

  private static boolean isInUiXmlFile(XmlTag context) {
    final PsiFile file = context.getContainingFile();
    return file instanceof XmlFile && UiBinderUtil.isUiXmlFile((XmlFile)file);
  }

  @Override
  public XmlAttributeDescriptor getAttributeDescriptor(String attributeName, XmlTag context) {
    final String localName = XmlUtil.findLocalNameByQualifiedName(attributeName);
    if (!UiBinderUtil.UI_FIELD_ATTRIBUTE.equals(localName) || !isInUiXmlFile(context)) return null;

    String prefix = XmlUtil.findPrefixByQualifiedName(attributeName);
    final String namespace = context.getNamespaceByPrefix(prefix);
    if (UiBinderUtil.UI_BINDER_NAMESPACE.equals(namespace)) {
      return new AnyXmlAttributeDescriptor(attributeName);
    }
    return null;
  }
}
