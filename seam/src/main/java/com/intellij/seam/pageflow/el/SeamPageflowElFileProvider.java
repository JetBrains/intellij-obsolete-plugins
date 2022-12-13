package com.intellij.seam.pageflow.el;

import com.intellij.psi.xml.XmlFile;
import com.intellij.seam.el.SeamElFileProvider;
import com.intellij.seam.pageflow.model.xml.PageflowDomModelManager;
import org.jetbrains.annotations.NotNull;

/**
 * @author Serega.Vasiliev
 */
public class SeamPageflowElFileProvider implements SeamElFileProvider {
  @Override
  public boolean isSeamElContainer(@NotNull XmlFile xmlFile) {
    return PageflowDomModelManager.getInstance(xmlFile.getProject()).isPageflow(xmlFile);
  }
}
