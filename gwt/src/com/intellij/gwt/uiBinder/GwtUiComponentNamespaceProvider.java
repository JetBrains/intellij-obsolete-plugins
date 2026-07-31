package com.intellij.gwt.uiBinder;

import com.intellij.javaee.ImplicitNamespaceDescriptorProvider;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.xml.XmlNSDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GwtUiComponentNamespaceProvider implements ImplicitNamespaceDescriptorProvider {
  @Override
  public XmlNSDescriptor getNamespaceDescriptor(@Nullable Module module, @NotNull String namespace, PsiFile file) {
    if (!namespace.startsWith(UiBinderUtil.URN_IMPORT_PREFIX) || module == null) return null;
    final XmlFile schema = GwtUiXmlSchemaProvider.findDefaultSchema(namespace, module);

    XmlNSDescriptor defaultNSDescriptor = null;
    if (schema != null) {
      final XmlDocument document = schema.getDocument();
      if (document != null) {
        defaultNSDescriptor = (XmlNSDescriptor)document.getMetaData();
      }
    }
    return new GwtUiComponentsNSDescriptor(module, namespace, defaultNSDescriptor);
  }
}
