package com.intellij.gwt.uiBinder;

import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlFileNSInfoProvider;
import com.intellij.xml.util.XmlUtil;
import org.jetbrains.annotations.NotNull;

public final class GwtUiHtmlSchemaProvider implements XmlFileNSInfoProvider {
  private static final String[][] HTML_NAMESPACES = {
    {"", XmlUtil.HTML_URI}
  };

  @Override
  public String[][] getDefaultNamespaces(@NotNull XmlFile file) {
    if (UiBinderUtil.isUiXmlFile(file)) {
      return HTML_NAMESPACES;
    }
    return null;

  }

  @Override
  public boolean overrideNamespaceFromDocType(@NotNull XmlFile file) {
    return false;
  }
}
