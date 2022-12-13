package com.intellij.seam.el.typedHandler;

import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.seam.el.SeamElFileProvider;

public class SeamElXmlTypedHandler extends BasicSeamElTypedHandler {
  @Override
  protected boolean isElContainerFile(final PsiFile originalFile) {
    return isSeamFacetDetected(originalFile) && isSeamConfig(originalFile);
  }

  private static boolean isSeamConfig(final PsiFile originalFile) {
    if (originalFile instanceof XmlFile) {
      final XmlFile xmlFile = (XmlFile)originalFile;

      for (SeamElFileProvider fileProvider : SeamElFileProvider.EP_NAME.getExtensionList()) {
        if (fileProvider.isSeamElContainer(xmlFile)) return true;
      }
    }

    return false;
  }
}
